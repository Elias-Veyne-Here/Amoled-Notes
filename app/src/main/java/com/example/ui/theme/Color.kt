package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// AMOLED True Black Monochrome Palette
val AmoledBlack = Color(0xFF000000)
val AmoledWhite = Color(0xFFFFFFFF)

// Surface Tints (White-Alpha overlays against #000000)
val SurfaceLevel1 = Color(0x14FFFFFF) // 8% white alpha - Cards & containers
val SurfaceLevel2 = Color(0x29FFFFFF) // 16% white alpha - Dialogs, sheets, pressed
val HairlineBorder = Color(0x1FFFFFFF) // 12% white alpha - Hairline borders & dividers

// Text & Element Tiers
val TextPrimary = Color(0xFFFFFFFF) // 100% white
val TextSecondary = Color(0x99FFFFFF) // 60% white
val TextTertiary = Color(0x73FFFFFF) // 45% white
val TextDisabled = Color(0x40FFFFFF) // 25% white

// Light Mode Inverses
val LightCanvas = Color(0xFFFFFFFF)
val LightSurfaceLevel1 = Color(0x0D000000) // 5% black
val LightSurfaceLevel2 = Color(0x1A000000) // 10% black
val LightHairlineBorder = Color(0x1E000000) // 12% black
val LightTextPrimary = Color(0xFF000000)
val LightTextSecondary = Color(0x99000000)
val LightTextTertiary = Color(0x73000000)
val LightTextDisabled = Color(0x40000000)
