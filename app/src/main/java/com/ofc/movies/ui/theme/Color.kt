package com.ofc.movies.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Cinematic Dark Palette
val DarkBackground = Color(0xFF0A0A0F)
val DarkSurface = Color(0xFF16161F)
val DarkSurfaceElevated = Color(0xFF22222E)
val DarkSurfaceHighlight = Color(0xFF2C2C3A)

// Accents
val NetflixRed = Color(0xFFE50914)
val NetflixRedDark = Color(0xFFB80610)
val GoldAccent = Color(0xFFFFD700)

// Text Colors
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA0A0B0)
val TextMuted = Color(0xFF6E6E82)

// Transparent Overlays & Shimmer
val ShimmerBase = Color(0xFF1C1C28)
val ShimmerHighlight = Color(0xFF2E2E3E)

// Gradient Brushes
val HeroGradient = Brush.verticalGradient(
    colors = listOf(
        Color.Transparent,
        DarkBackground.copy(alpha = 0.35f),
        DarkBackground.copy(alpha = 0.85f),
        DarkBackground
    )
)

val TopBarGradient = Brush.verticalGradient(
    colors = listOf(
        DarkBackground.copy(alpha = 0.95f),
        DarkBackground.copy(alpha = 0.70f),
        Color.Transparent
    )
)

val CardGradient = Brush.verticalGradient(
    colors = listOf(
        Color.Transparent,
        DarkSurface.copy(alpha = 0.92f)
    )
)
