package com.prayertimes.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun systemText(
    size: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    weight: FontWeight,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp
) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing
)

/** A complete type scale shared by Arabic and Latin UI. */
val Typography = Typography(
    displayLarge = systemText(57.sp, 64.sp, FontWeight.ExtraBold, (-0.25).sp),
    displayMedium = systemText(45.sp, 52.sp, FontWeight.Bold),
    displaySmall = systemText(36.sp, 44.sp, FontWeight.Bold),
    headlineLarge = systemText(32.sp, 40.sp, FontWeight.Bold),
    headlineMedium = systemText(28.sp, 36.sp, FontWeight.Bold),
    headlineSmall = systemText(24.sp, 32.sp, FontWeight.SemiBold),
    titleLarge = systemText(22.sp, 28.sp, FontWeight.Bold),
    titleMedium = systemText(16.sp, 23.sp, FontWeight.SemiBold, 0.1.sp),
    titleSmall = systemText(14.sp, 20.sp, FontWeight.SemiBold, 0.1.sp),
    bodyLarge = systemText(16.sp, 24.sp, FontWeight.Normal, 0.25.sp),
    bodyMedium = systemText(14.sp, 21.sp, FontWeight.Normal, 0.2.sp),
    bodySmall = systemText(12.sp, 18.sp, FontWeight.Normal, 0.2.sp),
    labelLarge = systemText(14.sp, 20.sp, FontWeight.SemiBold, 0.1.sp),
    labelMedium = systemText(12.sp, 17.sp, FontWeight.Medium, 0.25.sp),
    labelSmall = systemText(11.sp, 16.sp, FontWeight.Medium, 0.3.sp)
)

val SalatiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)
