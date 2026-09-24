package com.chinesegames.app.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.chinesegames.app.BuildConfig
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/** Данные вошедшего пользователя, безопасные для показа в интерфейсе. */
data class AccountProfile(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?
)

/**
 * Состояние Firebase Google Sign-In. Без `google-services.json` приложение
 * не падает: пользователю честно показывается, что вход ещё не настроен.
 */
sealed class AccountState {
    data object Loading : AccountState()
    data object NotConfigured : AccountState()
    data object SignedOut : AccountState()
    data class SignedIn(val profile: AccountProfile) : AccountState()
    data class Error(val message: String) : AccountState()
}

/** Итог нажатия «Войти через Google». */
sealed class SignInResult {
    data class Success(val profile: AccountProfile) : SignInResult()
    data object NotConfigured : SignInResult()
    data object Cancelled : SignInResult()
    data class Error(val message: String) : SignInResult()
}

/**
 * Firebase Auth + Credential Manager Google Sign-In.
 *
 * Конфигурация намеренно проверяется до вызова SDK: открытая сборка с
 * `google-services.json.example` остаётся рабочей, а production собирается
 * после добавления настоящего файла из Firebase Console.
 */
class AccountManager(
    context: Context,
    private val cloudSync: CloudSync
) {
    private val appContext = context.applicationContext
    private val credentialManager = CredentialManager.create(appContext)
    private var auth: FirebaseAuth? = null

    private val _state = MutableStateFlow<AccountState>(AccountState.Loading)
    val state: StateFlow<AccountState> = _state.asStateFlow()

    /** Firebase и OAuth настроены? */
    val isConfigured: Boolean
        get() = BuildConfig.CG_WEB_CLIENT_ID.isNotBlank() && FirebaseApp.initializeApp(appContext) != null

    init {
        if (!isConfigured) {
            _state.value = AccountState.NotConfigured
        } else {
            val firebaseAuth = FirebaseAuth.getInstance()
            auth = firebaseAuth
            firebaseAuth.addAuthStateListener { value ->
                val user = value.currentUser
                _state.value = user?.toProfile()?.let(AccountState::SignedIn) ?: AccountState.SignedOut
            }
            firebaseAuth.currentUser?.let { user ->
                _state.value = AccountState.SignedIn(user.toProfile())
            } ?: run { _state.value = AccountState.SignedOut }
        }
    }

    /**
     * Открыть системный лист Credential Manager и войти через Google.
     * Вызывать из Activity/экрана настроек; результат не содержит токенов.
     */
    suspend fun signIn(activity: Activity): SignInResult {
        if (!isConfigured) return SignInResult.NotConfigured
        val clientId = BuildConfig.CG_WEB_CLIENT_ID
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return SignInResult.Error("Google не вернул данные аккаунта")
            }
            val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val firebaseUser = requireNotNull(auth).signInWithCredential(
                GoogleAuthProvider.getCredential(token, null)
            ).await().user ?: return SignInResult.Error("Firebase не вернул пользователя")
            val profile = firebaseUser.toProfile()
            _state.value = AccountState.SignedIn(profile)
            // Вход — лучшее время, чтобы сразу объединить прогресс и покупки.
            cloudSync.sync(profile.uid)
            SignInResult.Success(profile)
        } catch (t: Throwable) {
            val message = t.message.orEmpty()
            if (message.contains("cancel", ignoreCase = true) || message.contains("cancelled", ignoreCase = true)) {
                SignInResult.Cancelled
            } else {
                _state.value = AccountState.Error(message.ifBlank { "Не удалось войти через Google" })
                SignInResult.Error(message.ifBlank { "Не удалось войти через Google" })
            }
        }
    }

    /** Выйти только из Firebase — системный аккаунт Google на телефоне не удаляется. */
    fun signOut() {
        try {
            auth?.signOut()
            _state.value = if (isConfigured) AccountState.SignedOut else AccountState.NotConfigured
        } catch (t: Throwable) {
            _state.value = AccountState.Error(t.message ?: "Не удалось выйти")
        }
    }

    /** Ручная синхронизация из настроек. */
    suspend fun syncNow(): CloudSyncResult {
        val profile = (_state.value as? AccountState.SignedIn)?.profile
            ?: return CloudSyncResult.NotSignedIn
        return cloudSync.sync(profile.uid)
    }

    /** Play Services установлены? Полезно объяснить редкую ошибку в старом Android. */
    fun playServicesAvailable(): Boolean =
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(appContext) == 0

    private fun FirebaseUser.toProfile() = AccountProfile(
        uid = uid,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl?.toString()
    )
}
