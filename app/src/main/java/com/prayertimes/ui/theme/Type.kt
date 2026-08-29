package com.prayertimes.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayertimes.R

private val SalatiSans = FontFamily(
    Font(R.font.zain_regular, FontWeight.Normal),
    Font(R.font.zain_bold, FontWeight.Bold),
    Font(R.font.zain_extra_bold, FontWeight.ExtraBold)
)

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
    displayLarge = salatiText(57.sp, 64.sp, FontWeight.ExtraBold, (-0.25).sp),
    displayMedium = salatiText(45.sp, 52.sp, FontWeight.Bold),
    displaySmall = salatiText(36.sp, 44.sp, FontWeight.Bold),
    headlineLarge = salatiText(32.sp, 40.sp, FontWeight.Bold),
    headlineMedium = salatiText(28.sp, 36.sp, FontWeight.Bold),
    headlineSmall = salatiText(24.sp, 32.sp, FontWeight.SemiBold),
    titleLarge = salatiText(22.sp, 28.sp, FontWeight.Bold),
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
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)
