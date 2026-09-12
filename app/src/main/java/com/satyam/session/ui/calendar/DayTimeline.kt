package com.satyam.session.ui.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.satyam.session.data.model.ActivityBucket
import com.satyam.session.data.model.Session
import com.satyam.session.ui.components.SessionBlock
import com.satyam.session.util.TimeUtils
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val HOUR_HEIGHT = 60.dp
private val TIME_LABEL_WIDTH = 56.dp

/**
 * Google Calendar-style 24-hour vertical timeline.
 * Sessions are rendered as colored blocks positioned by their start time,
 * with height proportional to duration.
 */
@Composable
fun DayTimeline(
    sessions: List<Session>,
    buckets: Map<Long, ActivityBucket>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val hourHeightPx = with(LocalDensity.current) { HOUR_HEIGHT.toPx() }
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h a") }

    Row(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // ── Time labels column ───────────────────────────────────────
        Column(modifier = Modifier.width(TIME_LABEL_WIDTH)) {
            for (hour in 0..23) {
                Box(
                    modifier = Modifier
                        .height(HOUR_HEIGHT)
                        .fillMaxWidth()
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = LocalTime.of(hour, 0).format(timeFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.offset(y = (-6).dp)
                    )
                }
            }
        }

        // ── Timeline content area ────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .height(HOUR_HEIGHT * 24)
        ) {
            // Hour divider lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (hour in 0..23) {
                    val y = hour * hourHeightPx
                    drawLine(
                        color = dividerColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }
            }

            // Session blocks
            sessions.forEach { session ->
                val startHour = TimeUtils.getHourOfDay(session.startTime)
                val durationHours = session.durationMs / 3_600_000f
                val topOffset = (startHour * HOUR_HEIGHT.value).dp
                val blockHeight = (durationHours * HOUR_HEIGHT.value).dp

                SessionBlock(
                    session = session,
                    bucket = buckets[session.bucketId],
                    height = blockHeight,
                    modifier = Modifier
                        .offset(y = topOffset)
                        .padding(horizontal = 2.dp, vertical = 1.dp)
                )
            }
        }
    }
}
