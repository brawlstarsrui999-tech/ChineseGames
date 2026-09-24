import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Firebase подключается только в настоящей сборке с google-services.json.
// Репозиторий остаётся собираемым с placeholders для открытого кода.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

val localProps = Properties().apply {
    val propsFile = rootProject.file("local.properties")
    if (propsFile.exists()) propsFile.inputStream().use(::load)
}

fun privateProperty(name: String, fallback: String = ""): String =
    providers.gradleProperty(name).orNull ?: localProps.getProperty(name) ?: fallback

android {
    namespace = "com.chinesegames.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.chinesegames.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        vectorDrawables {
            useSupportLibrary = true
        }

        // Robokassa: никогда не коммитим секреты. Для release подпись рекомендуем
        // отдавать на сервер (ROBOKASSA_SIGN_URL), тогда Password #1 не живёт в APK.
        buildConfigField("String", "ROBOKASSA_LOGIN", "\"${privateProperty("ROBOKASSA_LOGIN")}\"")
        buildConfigField("String", "ROBOKASSA_PASSWORD1", "\"${privateProperty("ROBOKASSA_PASSWORD1")}\"")
        buildConfigField("String", "ROBOKASSA_PASSWORD2", "\"${privateProperty("ROBOKASSA_PASSWORD2")}\"")
        buildConfigField("boolean", "ROBOKASSA_TEST", privateProperty("ROBOKASSA_TEST", "true"))
        buildConfigField("String", "ROBOKASSA_SIGN_URL", "\"${privateProperty("ROBOKASSA_SIGN_URL")}\"")

        // По умолчанию — официальные тестовые идентификаторы Google AdMob.
        buildConfigField(
            "String", "ADMOB_APP_ID",
            "\"${privateProperty("ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")}\""
        )
        buildConfigField(
            "String", "ADMOB_BANNER_ID",
            "\"${privateProperty("ADMOB_BANNER_ID", "ca-app-pub-3940256099942544/6300978111")}\""
        )
        // OAuth web client ID из Firebase Console (для Google Sign-In).
        buildConfigField("String", "CG_WEB_CLIENT_ID", "\"${privateProperty("CG_WEB_CLIENT_ID")}\"")
        manifestPlaceholders["admobAppId"] = privateProperty(
            "ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713"
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.google.play.services.ads)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.ui.tooling)
}
