package com.prayertimes.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SalatiSans = FontFamily.SansSerif

private fun salatiText(
    size: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    weight: FontWeight,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp
) = TextStyle(
    fontFamily = SalatiSans,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing
)

/** A complete type scale shared by Arabic and Latin UI. */
val Typography = Typography(
    displayLarge = salatiText(52.sp, 60.sp, FontWeight.Bold, (-0.25).sp),
    displayMedium = salatiText(42.sp, 50.sp, FontWeight.Bold),
    displaySmall = salatiText(34.sp, 42.sp, FontWeight.SemiBold),
    headlineLarge = salatiText(30.sp, 38.sp, FontWeight.SemiBold),
    headlineMedium = salatiText(26.sp, 34.sp, FontWeight.SemiBold),
    headlineSmall = salatiText(23.sp, 30.sp, FontWeight.SemiBold),
    titleLarge = salatiText(21.sp, 28.sp, FontWeight.SemiBold),
    titleMedium = salatiText(16.sp, 23.sp, FontWeight.SemiBold, 0.1.sp),
    titleSmall = salatiText(14.sp, 20.sp, FontWeight.SemiBold, 0.1.sp),
    bodyLarge = salatiText(16.sp, 24.sp, FontWeight.Normal, 0.25.sp),
    bodyMedium = salatiText(14.sp, 21.sp, FontWeight.Normal, 0.2.sp),
    bodySmall = salatiText(12.sp, 18.sp, FontWeight.Normal, 0.2.sp),
    labelLarge = salatiText(14.sp, 20.sp, FontWeight.SemiBold, 0.1.sp),
    labelMedium = salatiText(12.sp, 17.sp, FontWeight.Medium, 0.25.sp),
    labelSmall = salatiText(11.sp, 16.sp, FontWeight.Medium, 0.3.sp)
)

val SalatiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
