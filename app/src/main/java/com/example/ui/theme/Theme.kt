package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.model.AppTheme

@Immutable
data class AllenaMenteStyleTokens(
    val theme: AppTheme,
    val isDark: Boolean,
    val cardCornerRadius: Dp,
    val cardBorderWidth: Dp,
    val nightCalmAudio: Boolean
)

val LocalPillarColors = staticCompositionLocalOf { LightPillarColors }
val LocalStyleTokens = staticCompositionLocalOf {
    AllenaMenteStyleTokens(
        theme = AppTheme.ZEN_PEBBLE,
        isDark = false,
        cardCornerRadius = 22.dp,
        cardBorderWidth = 1.dp,
        nightCalmAudio = false
    )
}

@Composable
fun AllenaMenteTheme(
    appTheme: AppTheme = AppTheme.ZEN_PEBBLE,
    content: @Composable () -> Unit
) {
    val isDark = appTheme == AppTheme.OBSIDIAN_DARK
    val colorScheme = when (appTheme) {
        AppTheme.ZEN_PEBBLE -> ZenPebbleColorScheme
        AppTheme.WARM_GLASS -> WarmGlassColorScheme
        AppTheme.NEO_BRUTALIST -> NeoBrutalistColorScheme
        AppTheme.OBSIDIAN_DARK -> ObsidianDarkColorScheme
    }
    val pillarColors = if (isDark) DarkPillarColors else LightPillarColors
    val styleTokens = when (appTheme) {
        AppTheme.ZEN_PEBBLE -> AllenaMenteStyleTokens(appTheme, false, 22.dp, 1.dp, false)
        AppTheme.WARM_GLASS -> AllenaMenteStyleTokens(appTheme, false, 24.dp, 1.dp, false)
        AppTheme.NEO_BRUTALIST -> AllenaMenteStyleTokens(appTheme, false, 10.dp, 2.dp, false)
        AppTheme.OBSIDIAN_DARK -> AllenaMenteStyleTokens(appTheme, true, 22.dp, 1.dp, true)
    }

    val shapes = Shapes(
        small = RoundedCornerShape(if (appTheme == AppTheme.NEO_BRUTALIST) 6.dp else 12.dp),
        medium = RoundedCornerShape(styleTokens.cardCornerRadius),
        large = RoundedCornerShape(if (appTheme == AppTheme.NEO_BRUTALIST) 12.dp else 28.dp)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalPillarColors provides pillarColors,
        LocalStyleTokens provides styleTokens
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AllenaMenteTypography,
            shapes = shapes,
            content = content
        )
    }
}
