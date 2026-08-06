package com.vythera.omniapps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEW WRAPPERS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ThemePreviewWrapper(theme: AppThemeColors, content: @Composable () -> Unit) {
    val colorScheme = if (theme.isDark) darkColorScheme(
        primary              = theme.accent,
        primaryContainer     = theme.accentContainer,
        onPrimaryContainer   = theme.onAccentContainer,
        secondary            = theme.accentAlt,
        tertiary             = theme.accentTertiary,
        tertiaryContainer    = theme.accentTertiaryContainer,
        surface              = theme.bgSurface,
        surfaceContainer     = theme.bgSurfaceAlt,
        surfaceContainerHigh = theme.bgSurfaceHigh,
        background           = theme.bgPrimary,
        onBackground         = theme.textPrimary,
        onSurface            = theme.textPrimary,
        onSurfaceVariant     = theme.textSecondary,
        outline              = theme.border,
        outlineVariant       = theme.borderVariant
    ) else lightColorScheme(
        primary              = theme.accent,
        primaryContainer     = theme.accentContainer,
        onPrimaryContainer   = theme.onAccentContainer,
        secondary            = theme.accentAlt,
        tertiary             = theme.accentTertiary,
        tertiaryContainer    = theme.accentTertiaryContainer,
        surface              = theme.bgSurface,
        surfaceContainer     = theme.bgSurfaceAlt,
        surfaceContainerHigh = theme.bgSurfaceHigh,
        background           = theme.bgPrimary,
        onBackground         = theme.textPrimary,
        onSurface            = theme.textPrimary,
        onSurfaceVariant     = theme.textSecondary,
        outline              = theme.border,
        outlineVariant       = theme.borderVariant
    )
    CompositionLocalProvider(
        LocalTheme provides theme,
        LocalStrings provides AppStrings()
    ) {
        MaterialTheme(colorScheme = colorScheme) {
            Box(
                modifier         = Modifier.background(theme.bgPrimary),
                contentAlignment = Alignment.Center
            ) { content() }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DOCK ITEM
// ─────────────────────────────────────────────────────────────────────────────

// DockItem previews removed — NavigationBarItem is used directly via FloatingNavBar

// ─────────────────────────────────────────────────────────────────────────────
// FLOATING NAV BAR  — one preview per theme
// ─────────────────────────────────────────────────────────────────────────────

@Preview(name = "NavBar · Dark", showBackground = true, backgroundColor = 0xFF141218, widthDp = 420)
@Composable
private fun PreviewNavBarDark() {
    ThemePreviewWrapper(DarkTheme) {
        Box(
            modifier         = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingNavBar(
                selectedTab = VAppTab.HOME,

                onTabSelect = {},
                updateCount = 0
            )
        }
    }
}

@Preview(name = "NavBar · AMOLED", showBackground = true, backgroundColor = 0xFF000000, widthDp = 420)
@Composable
private fun PreviewNavBarAmoled() {
    ThemePreviewWrapper(AmoledTheme) {
        Box(
            modifier         = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingNavBar(
                selectedTab = VAppTab.PROFILE,

                onTabSelect = {},
                updateCount = 0
            )
        }
    }
}

@Preview(name = "NavBar · Sunset", showBackground = true, backgroundColor = 0xFF1C1107, widthDp = 420)
@Composable
private fun PreviewNavBarSunset() {
    ThemePreviewWrapper(SunsetTheme) {
        Box(
            modifier         = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingNavBar(
                selectedTab = VAppTab.SETTINGS,

                onTabSelect = {},
                updateCount = 0
            )
        }
    }
}

@Preview(name = "NavBar · Light", showBackground = true, backgroundColor = 0xFFFDFCFF, widthDp = 420)
@Composable
private fun PreviewNavBarLight() {
    ThemePreviewWrapper(LightTheme) {
        Box(
            modifier         = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingNavBar(
                selectedTab = VAppTab.HOME,

                onTabSelect = {},
                updateCount = 0
            )
        }
    }
}

@Preview(name = "NavBar · Badge", showBackground = true, backgroundColor = 0xFF141218, widthDp = 420)
@Composable
private fun PreviewNavBarWithBadge() {
    ThemePreviewWrapper(DarkTheme) {
        Box(
            modifier         = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingNavBar(
                selectedTab = VAppTab.INSTALLED,

                onTabSelect = {},
                updateCount = 5
            )
        }
    }
}
