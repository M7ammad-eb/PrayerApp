package com.prayertimes.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

/**
 * A low-cost page entrance that never composes the outgoing and incoming screens together.
 * This matters for pages such as the compass, calendar, and settings, which each do substantial
 * work while composed.
 */
@Composable
fun LightweightPageEntry(
    animationKey: Any?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val alpha = remember(animationKey) { Animatable(0.88f) }
    val translationY = remember(animationKey) { Animatable(12f) }

    LaunchedEffect(animationKey) {
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
            )
        }
        launch {
            translationY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 170, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = alpha.value
                this.translationY = translationY.value
            }
    ) {
        content()
    }
}
