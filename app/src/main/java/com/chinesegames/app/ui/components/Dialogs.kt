package com.chinesegames.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chinesegames.app.data.Deck
import com.chinesegames.app.data.Word
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary
import com.chinesegames.app.ui.theme.VividPurple

/** Каркас всех диалогов приложения — тот же «фиолетовый неон», что и у экранов. */
@Composable
fun CgDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .imePadding()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(CG.surface)
                )
                .border(1.dp, Brush.linearGradient(CG.cardBorder), RoundedCornerShape(28.dp))
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
            content = content
        )
    }
}

/** Текстовое поле в стиле приложения. */
@Composable
fun CgTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    imeAction: ImeAction = ImeAction.Next,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary) },
        placeholder = placeholder?.let { { Text(it, color = TextMuted) } },
        textStyle = textStyle,
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        leadingIcon = leading,
        trailingIcon = trailing,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            imeAction = imeAction
        ),
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = VividPurple,
            unfocusedBorderColor = LavenderGlow.copy(alpha = 0.3f),
            cursorColor = VividPurple,
            focusedContainerColor = Color.White.copy(alpha = 0.05f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
        )
    )
}

private val deckEmojis = listOf("📚", "🀄", "🐼", "🏮", "🍜", "🌸", "🐉", "🎯", "🔥", "⭐", "🎵", "🌊")

/** Создание и редактирование папки. */
@Composable
fun DeckEditorDialog(
    deck: Deck?,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String) -> Unit
) {
    var name by remember { mutableStateOf(deck?.name ?: "") }
    var emoji by remember { mutableStateOf(deck?.emoji ?: "📚") }

    CgDialog(onDismiss = onDismiss) {
        Text(
            text = if (deck == null) "Новая папка" else "Изменить папку",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Папка — это тема: «Еда», «Путешествия», «HSK 3»…",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
        Spacer(Modifier.height(18.dp))

        CgTextField(
            value = name,
            onValueChange = { name = it.take(40) },
            label = "Название папки",
            placeholder = "Например: Еда и напитки",
            imeAction = ImeAction.Done
        )
        Spacer(Modifier.height(18.dp))

        Text("Иконка", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            deckEmojis.take(6).forEach { e -> EmojiDot(e, emoji == e) { emoji = e } }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            deckEmojis.drop(6).forEach { e -> EmojiDot(e, emoji == e) { emoji = e } }
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GhostButton("Отмена", modifier = Modifier.weight(1f), onClick = onDismiss)
            GradientButton(
                text = if (deck == null) "Создать" else "Сохранить",
                modifier = Modifier.weight(1f),
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(name.trim(), emoji)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun EmojiDot(emoji: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(
                if (selected) Brush.horizontalGradient(CG.primaryGradient)
                else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.04f)))
            )
            .border(
                1.dp,
                if (selected) Color.Transparent else LavenderGlow.copy(alpha = 0.25f),
                CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 22.sp)
    }
}

/** Добавление и редактирование слова. */
@Composable
fun WordEditorDialog(
    word: Word?,
    onDismiss: () -> Unit,
    onSpeak: (String) -> Unit,
    onSave: (hanzi: String, pinyin: String, translation: String) -> Unit
) {
    var hanzi by remember { mutableStateOf(word?.hanzi ?: "") }
    var pinyin by remember { mutableStateOf(word?.pinyin ?: "") }
    var translation by remember { mutableStateOf(word?.translation ?: "") }

    val canSave = hanzi.isNotBlank() && translation.isNotBlank()

    CgDialog(onDismiss = onDismiss) {
        Text(
            text = if (word == null) "Новое слово" else "Изменить слово",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Иероглиф · пиньинь · перевод. Всё вводите сами.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
        Spacer(Modifier.height(18.dp))

        CgTextField(
            value = hanzi,
            onValueChange = { hanzi = it.take(20) },
            label = "Иероглиф",
            placeholder = "你好",
            textStyle = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(12.dp))
        CgTextField(
            value = pinyin,
            onValueChange = { pinyin = it.take(40) },
            label = "Пиньинь (необязательно)",
            placeholder = "nǐ hǎo"
        )
        Spacer(Modifier.height(12.dp))
        CgTextField(
            value = translation,
            onValueChange = { translation = it.take(60) },
            label = "Перевод на русский",
            placeholder = "привет",
            imeAction = ImeAction.Done
        )

        if (hanzi.isNotBlank()) {
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, LavenderGlow.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text = hanzi, fontSize = 30.sp, color = TextPrimary)
                    if (pinyin.isNotBlank()) {
                        Text(text = pinyin, style = MaterialTheme.typography.bodyMedium, color = LavenderGlow)
                    }
                    if (translation.isNotBlank()) {
                        Text(
                            text = translation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                CircleIconButton(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Озвучить",
                    onClick = { onSpeak(hanzi) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GhostButton("Отмена", modifier = Modifier.weight(1f), onClick = onDismiss)
            GradientButton(
                text = if (word == null) "Добавить" else "Сохранить",
                modifier = Modifier.weight(1f),
                enabled = canSave,
                onClick = {
                    onSave(hanzi.trim(), pinyin.trim(), translation.trim())
                    onDismiss()
                }
            )
        }
    }
}

/** Подтверждение удаления. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Удалить",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    CgDialog(onDismiss = onDismiss) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GhostButton("Отмена", modifier = Modifier.weight(1f), onClick = onDismiss)
            GradientButton(
                text = confirmText,
                modifier = Modifier.weight(1f),
                colors = CG.dangerGradient,
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            )
        }
    }
}

/** Кнопка-«иконка» для строк с действиями. */
@Composable
fun ActionRowSpacer() = Spacer(Modifier.width(8.dp))
