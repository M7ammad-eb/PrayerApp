package com.prayertimes.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/**
 * Shared Material 3 Expressive layout and motion tokens.
 *
 * Keeping these values in one place prevents individual screens from slowly drifting into
 * unrelated corner radii, spacing, and animation timings again.
 */
@Immutable
object ExpressiveSpacing {
    val screenHorizontal = 16.dp
    val screenTop = 8.dp
    val section = 16.dp
    val related = 8.dp
    val card = PaddingValues(horizontal = 18.dp, vertical = 16.dp)
    val compactCard = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
}

object ExpressiveMotion {
    fun <T> emphasized() = spring<T>(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> standard() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}
