package com.chinesegames.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chinesegames.app.ui.theme.CgPaletteState
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.MidnightPurple
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary

/**
 * Нижняя навигация приложения: Главная, Словарь, Игры, Курс, Прогресс, Ещё.
 * Все основные разделы теперь живут здесь, поэтому главный экран не завален
 * кнопками и текстом.
 */
@Composable
fun MainBottomBar(
    current: MainTab?,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val night = CgPaletteState.night

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(LavenderGlow.copy(alpha = if (night) 0.28f else 0.35f))
        )
        NavigationBar(
            containerColor = MidnightPurple,
            tonalElevation = 0.dp,
            contentColor = TextPrimary
        ) {
            MainTab.entries.forEach { tab ->
                val selected = tab == current
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(tab) },
                    alwaysShowLabel = true,
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            modifier = Modifier.size(22.dp),
                            tint = if (selected) tab.accent else TextMuted
                        )
                    },
                    label = {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (selected) tab.accent else TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = tab.accent,
                        selectedTextColor = tab.accent,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = tab.accent.copy(alpha = if (night) 0.20f else 0.22f)
                    )
                )
            }
        }
    }
}

/** Цвет подложки нижней панели — для Scaffold, чтобы не было «белой полосы». */
val BottomBarSurface: Color
    get() = MidnightPurple
