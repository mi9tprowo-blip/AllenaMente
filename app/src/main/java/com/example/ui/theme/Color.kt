package com.example.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// 4 Cognitive Pillar Signature Accents
val PillarLogicAmber = Color(0xFFD97706)
val PillarLogicAmberLight = Color(0xFFF59E0B)
val PillarMemoryTeal = Color(0xFF0D9488)
val PillarMemoryTealLight = Color(0xFF14B8A6)
val PillarReflexRose = Color(0xFFE11D48)
val PillarReflexRoseLight = Color(0xFFF43F5E)
val PillarEmpathyIndigo = Color(0xFF4F46E5)
val PillarEmpathyIndigoLight = Color(0xFF6366F1)

@Immutable
data class PillarColors(
    val logic: Color,
    val memory: Color,
    val reflex: Color,
    val empathy: Color,
    val streakIce: Color,
    val streakFlame: Color,
    val success: Color,
    val warning: Color
)

val LightPillarColors = PillarColors(
    logic = PillarLogicAmber,
    memory = PillarMemoryTeal,
    reflex = PillarReflexRose,
    empathy = PillarEmpathyIndigo,
    streakIce = Color(0xFF0284C7),
    streakFlame = Color(0xFFEA580C),
    success = Color(0xFF16A34A),
    warning = Color(0xFFD97706)
)

val DarkPillarColors = PillarColors(
    logic = Color(0xFFFBBF24),
    memory = Color(0xFF2DD4BF),
    reflex = Color(0xFFFB7185),
    empathy = Color(0xFF818CF8),
    streakIce = Color(0xFF38BDF8),
    streakFlame = Color(0xFFFB923C),
    success = Color(0xFF4ADE80),
    warning = Color(0xFFFBBF24)
)

// Theme 1: Zen Garden Pebble (Default — Organic warm stone, sage & deep slate)
val ZenPebbleColorScheme = lightColorScheme(
    primary = Color(0xFF1E5E53),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDECE4),
    onPrimaryContainer = Color(0xFF062C25),
    secondary = Color(0xFF4F5D57),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDE5DF),
    onSecondaryContainer = Color(0xFF18241F),
    tertiary = Color(0xFFB45309),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFDE6C4),
    onTertiaryContainer = Color(0xFF3B1900),
    background = Color(0xFFF5F3EE),
    onBackground = Color(0xFF161C19),
    surface = Color(0xFFFCFBF8),
    onSurface = Color(0xFF161C19),
    surfaceVariant = Color(0xFFE8E4DC),
    onSurfaceVariant = Color(0xFF444B47),
    outline = Color(0xFFC8C2B8),
    outlineVariant = Color(0xFFDFDAD0),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF)
)

// Theme 2: Warm Liquid Glass (Sunlit cream, terracotta coral & indigo depth)
val WarmGlassColorScheme = lightColorScheme(
    primary = Color(0xFFC2410C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE4D6),
    onPrimaryContainer = Color(0xFF431407),
    secondary = Color(0xFF4338CA),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF1E1B4B),
    tertiary = Color(0xFF0F766E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF042F2E),
    background = Color(0xFFFFF8F3),
    onBackground = Color(0xFF1F1612),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1612),
    surfaceVariant = Color(0xFFF5E6DC),
    onSurfaceVariant = Color(0xFF574238),
    outline = Color(0xFFD9C0B0),
    outlineVariant = Color(0xFFEDD9CC),
    error = Color(0xFFBE123C),
    onError = Color(0xFFFFFFFF)
)

// Theme 3: Neo-Brutalist Raw (Crisp bone paper, electric cobalt & ink black strokes)
val NeoBrutalistColorScheme = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = Color(0xFF0F172A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFEF08A),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFFDB2777),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFCE7F3),
    onTertiaryContainer = Color(0xFF500724),
    background = Color(0xFFFAF9F2),
    onBackground = Color(0xFF0A0A0A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFFECEAE0),
    onSurfaceVariant = Color(0xFF262626),
    outline = Color(0xFF0A0A0A),
    outlineVariant = Color(0xFF262626),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF)
)

// Theme 4: Obsidian Dark (Deep nocturnal slate, luminous teal & night-calm warmth)
val ObsidianDarkColorScheme = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF134E4A),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFF818CF8),
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF312E81),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF0B1120),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF131C31),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)
