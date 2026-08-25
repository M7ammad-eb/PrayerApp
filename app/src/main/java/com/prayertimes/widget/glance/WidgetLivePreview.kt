package com.prayertimes.widget.glance

import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.prayertimes.data.preferences.AppPrayerSettings

/**
 * Renders the real [PrayerGlanceWidget] composable - not a hand-copied mockup - at the given
 * footprint, fed with the settings screen's own live (possibly unsaved) [settings]. Running
 * [WidgetContent] through Glance's actual [GlanceRemoteViews.compose] pipeline means whatever
 * appears here is guaranteed pixel-identical to what the home screen widget would show at this
 * size, and can never silently drift the way a separately maintained mock can.
 *
 * The real widget is tap-to-open-the-app and carries its own on-widget refresh button; since this
 * is a settings-screen preview and not a real installed widget instance, an invisible overlay
 * swallows all touches so neither one fires from in here.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@Composable
fun LiveGlanceWidgetPreview(settings: AppPrayerSettings, height: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val glanceRemoteViews = remember { GlanceRemoteViews() }

    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(height)) {
        val size = DpSize(maxWidth, maxHeight)
        var remoteViews by remember { mutableStateOf<RemoteViews?>(null) }

        LaunchedEffect(settings, size) {
            val data = PrayerGlanceWidget().buildGlanceWidgetData(context, settings)
            remoteViews = glanceRemoteViews.compose(context, size) { WidgetContent(data) }.remoteViews
        }

        remoteViews?.let { rv ->
            AndroidView(
                modifier = Modifier.matchParentSize(),
                factory = { ctx -> FrameLayout(ctx) },
                update = { container ->
                    container.removeAllViews()
                    container.addView(rv.apply(context, container))
                }
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent().changes.forEach { it.consume() }
                            }
                        }
                    }
            )
        }
    }
}
