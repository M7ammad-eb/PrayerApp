package com.prayertimes.widget.glance

import android.widget.FrameLayout
import android.widget.RemoteViews
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.prayertimes.data.preferences.AppPrayerSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
fun LiveGlanceWidgetPreview(settings: AppPrayerSettings, size: DpSize, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val glanceRemoteViews = remember { GlanceRemoteViews() }
    var remoteViews by remember { mutableStateOf<RemoteViews?>(null) }

    Box(modifier = modifier.size(size.width, size.height)) {
        LaunchedEffect(settings, size) {
            val data = withContext(Dispatchers.Default) {
                PrayerGlanceWidget().buildGlanceWidgetData(context, settings)
            }
            remoteViews = glanceRemoteViews.compose(context, size) { WidgetContent(data) }.remoteViews
        }

        remoteViews?.let { rv ->
            AndroidView(
                modifier = Modifier.matchParentSize(),
                factory = { ctx -> FrameLayout(ctx) },
                update = { container ->
                    container.removeAllViews()
                    // WidgetContent already performs explicit RTL child ordering because launcher
                    // RemoteViews do not reliably mirror Glance rows. Inside this Arabic Compose
                    // screen, however, AndroidView makes the inflated hierarchy inherit RTL and
                    // mirrors it a second time. Pin the preview hierarchy to LTR so it exercises
                    // the same explicit ordering as the actual launcher widget.
                    container.layoutDirection = View.LAYOUT_DIRECTION_LTR
                    val inflated = rv.apply(context, container)
                    forceLayoutDirectionLtr(inflated)
                    container.addView(inflated)
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

private fun forceLayoutDirectionLtr(view: View) {
    view.layoutDirection = View.LAYOUT_DIRECTION_LTR
    if (view is ViewGroup) {
        for (index in 0 until view.childCount) forceLayoutDirectionLtr(view.getChildAt(index))
    }
}
