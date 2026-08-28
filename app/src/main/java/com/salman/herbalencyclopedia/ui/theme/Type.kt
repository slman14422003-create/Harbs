package com.salman.herbalencyclopedia.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Default system font renders Arabic script correctly; a custom Arabic
// webfont (e.g. Cairo/Tajawal) can be dropped into res/font and referenced
// here later for closer brand match.
val HerbalTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
)


fun Typography.scaled(level: Int): Typography {
    val factor = when (level.coerceIn(0,2)) { 1 -> 1.15f; 2 -> 1.30f; else -> 1f }
    fun TextStyle.s() = copy(fontSize = fontSize * factor, lineHeight = lineHeight * factor)
    return copy(headlineLarge=headlineLarge.s(), headlineMedium=headlineMedium.s(), titleLarge=titleLarge.s(), titleMedium=titleMedium.s(), bodyLarge=bodyLarge.s(), bodyMedium=bodyMedium.s(), labelLarge=labelLarge.s())
}
