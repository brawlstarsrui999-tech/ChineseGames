package com.chinesegames.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chinesegames.app.ui.components.GhostButton
import com.chinesegames.app.ui.components.GradientButton
import com.chinesegames.app.ui.components.VSpace
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.PixelType
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary

private data class GuidePage(
    val emoji: String,
    val title: String,
    val description: String,
    val points: List<String>
)

/**
 * Короткое знакомство, которое появляется один раз при первом запуске.
 * Оно объясняет основные разделы до того, как пользователь начнёт учиться,
 * а из настроек его можно открыть повторно.
 */
@Composable
fun FirstLaunchGuide(
    visible: Boolean,
    onFinish: () -> Unit
) {
    if (!visible) return

    val pages = remember {
        listOf(
            GuidePage(
                emoji = "你好",
                title = "Добро пожаловать в ChineseGames",
                description = "Учите китайский в своём темпе: все уроки, словарь и игры доступны бесплатно.",
                points = listOf(
                    "Главные разделы находятся в панели внизу экрана",
                    "Настройки всегда можно изменить позже"
                )
            ),
            GuidePage(
                emoji = "📚",
                title = "Курсы HSK",
                description = "Во вкладке «Курс» вас ждут уровни HSK 1–7, разбитые на короткие понятные темы.",
                points = listOf(
                    "Изучайте слова небольшими группами",
                    "Закрепляйте тему мини-играми и предложениями"
                )
            ),
            GuidePage(
                emoji = "🎮",
                title = "Учитесь, играя",
                description = "Во вкладке «Игры» выбирайте тренировку: пары, спринт, пиньинь, тоны и другие режимы.",
                points = listOf(
                    "Можно выбрать нужные папки со словами",
                    "Результаты и личные рекорды сохраняются"
                )
            ),
            GuidePage(
                emoji = "🗂️",
                title = "Словарь и повторение",
                description = "Во вкладке «Словарь» доступны HSK-наборы, свои папки, избранное и сложные слова.",
                points = listOf(
                    "Добавляйте собственные слова и CSV-файлы",
                    "Приложение помогает чаще повторять трудные слова"
                )
            ),
            GuidePage(
                emoji = "⚙️",
                title = "Всё готово",
                description = "В «Настройках» можно выбрать тему, размер текста, звук и другие удобные параметры.",
                points = listOf(
                    "Прогресс смотрите во вкладке «Статистика»",
                    "Начните с курса HSK 1 или выберите любимую игру"
                )
            )
        )
    }
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = pages[pageIndex]
    val isLast = pageIndex == pages.lastIndex

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .padding(22.dp)
                .widthIn(max = 460.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(CG.cardGradient))
                .border(1.dp, LavenderGlow.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
                .padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${pageIndex + 1} / ${pages.size}",
                    style = PixelType.chip,
                    color = GoldAccent,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
                VSpace(2.dp)
                Text(text = page.emoji, fontSize = 52.sp, textAlign = TextAlign.Center)
                VSpace(10.dp)
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                VSpace(8.dp)
                Text(
                    text = page.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                VSpace(18.dp)
                page.points.forEach { point ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "✦", color = GoldAccent, fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = point,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                VSpace(16.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GhostButton(
                        text = if (pageIndex == 0) "Пропустить" else "Назад",
                        modifier = Modifier.weight(1f)
                    ) {
                        if (pageIndex == 0) onFinish() else pageIndex -= 1
                    }
                    GradientButton(
                        text = if (isLast) "Начать" else "Далее",
                        modifier = Modifier.weight(1f),
                        colors = CG.primaryGradient
                    ) {
                        if (isLast) onFinish() else pageIndex += 1
                    }
                }
                VSpace(4.dp)
                Text(
                    text = "Этот гид можно открыть повторно в настройках.",
                    style = PixelType.caption,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
