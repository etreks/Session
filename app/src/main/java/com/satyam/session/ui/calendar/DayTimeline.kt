package com.satyam.session.ui.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.satyam.session.data.model.ActivityBucket
import com.satyam.session.data.model.Session
import com.satyam.session.ui.components.SessionBlock
import com.satyam.session.util.TimeUtils
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME_LABEL_WIDTH = 54.dp
private const val MIN_HOUR_HEIGHT_DP = 48f
private const val MAX_HOUR_HEIGHT_DP = 160f
private const val DEFAULT_HOUR_HEIGHT_DP = 72f

data class TimelineLayoutSession(
    val session: Session,
    val colIndex: Int,
    val totalCols: Int
)

/**
 * Google Calendar-style 24-hour vertical timeline.
 * Features:
 *  - Collision detection & side-by-side columns for overlapping sessions
 *  - Pinch-to-zoom gesture & floating zoom controls (48dp - 160dp per hour)
 *  - Proportional heights with smart min-height for short activities
 *  - Auto-scroll to current time on first load
 */
@Composable
fun DayTimeline(
    sessions: List<Session>,
    buckets: Map<Long, ActivityBucket>,
    modifier: Modifier = Modifier
) {
    var hourHeightDpVal by remember { mutableFloatStateOf(DEFAULT_HOUR_HEIGHT_DP) }
    val hourHeightDp = hourHeightDpVal.dp
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeightDp.toPx() }
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h a") }

    // Auto-scroll to current time (or first session) on open
    var hasAutoScrolled by remember { mutableStateOf(false) }
    LaunchedEffect(hourHeightDpVal) {
        if (!hasAutoScrolled) {
            val nowHour = LocalTime.now().hour
            val targetHour = if (sessions.isNotEmpty()) {
                val firstHour = TimeUtils.getHourOfDay(sessions.first().startTime).toInt()
                (firstHour - 1).coerceAtLeast(0)
            } else {
                (nowHour - 1).coerceAtLeast(0)
            }
            val scrollY = with(density) { (targetHour * hourHeightDp).toPx() }.toInt()
            scrollState.scrollTo(scrollY)
            hasAutoScrolled = true
        }
    }

    val positionedSessions = remember(sessions) {
        calculateTimelineLayout(sessions)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val newHeight = (hourHeightDpVal * zoom).coerceIn(MIN_HOUR_HEIGHT_DP, MAX_HOUR_HEIGHT_DP)
                        hourHeightDpVal = newHeight
                    }
                }
                .verticalScroll(scrollState)
        ) {
            // ── Time labels column ───────────────────────────────────────
            Column(modifier = Modifier.width(TIME_LABEL_WIDTH)) {
                for (hour in 0..23) {
                    Box(
                        modifier = Modifier
                            .height(hourHeightDp)
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = LocalTime.of(hour, 0).format(timeFormatter),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.offset(y = (-7).dp)
                        )
                    }
                }
            }

            // ── Timeline content area ────────────────────────────────────
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(hourHeightDp * 24)
            ) {
                val availableWidth = maxWidth

                // Hour divider lines & current time red indicator
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

                    // Red Current Time Line
                    val currentFractionalHour = LocalTime.now().hour + LocalTime.now().minute / 60f
                    val currentY = currentFractionalHour * hourHeightPx
                    drawLine(
                        color = Color(0xFFEF4444),
                        start = Offset(0f, currentY),
                        end = Offset(size.width, currentY),
                        strokeWidth = 2f
                    )
                    drawCircle(
                        color = Color(0xFFEF4444),
                        radius = 4f,
                        center = Offset(0f, currentY)
                    )
                }

                // Session blocks positioned side-by-side without collision
                positionedSessions.forEach { item ->
                    val session = item.session
                    val startHour = TimeUtils.getHourOfDay(session.startTime)
                    val durationHours = session.durationMs / 3_600_000f
                    val topOffset = (startHour * hourHeightDp.value).dp

                    // Calculate proportional height with a comfortable min-height
                    val naturalHeight = (durationHours * hourHeightDp.value).dp
                    val blockHeight = naturalHeight.coerceAtLeast(30.dp)

                    // Side-by-side column width and left offset
                    val colWidth = availableWidth / item.totalCols
                    val leftOffset = item.colIndex * colWidth

                    SessionBlock(
                        session = session,
                        bucket = buckets[session.bucketId],
                        height = blockHeight,
                        modifier = Modifier
                            .width(colWidth)
                            .offset(x = leftOffset, y = topOffset)
                            .padding(horizontal = 2.dp, vertical = 1.dp)
                    )
                }
            }
        }

        // ── Floating Zoom Controls ────────────────────────────────────
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        hourHeightDpVal = (hourHeightDpVal - 16f).coerceIn(MIN_HOUR_HEIGHT_DP, MAX_HOUR_HEIGHT_DP)
                    },
                    containerColor = Color.Transparent,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Zoom out",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
                SmallFloatingActionButton(
                    onClick = {
                        hourHeightDpVal = (hourHeightDpVal + 16f).coerceIn(MIN_HOUR_HEIGHT_DP, MAX_HOUR_HEIGHT_DP)
                    },
                    containerColor = Color.Transparent,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom in",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Google Calendar Interval Collision Algorithm
 * Assigns non-overlapping columns to concurrent events.
 */
fun calculateTimelineLayout(sessions: List<Session>): List<TimelineLayoutSession> {
    if (sessions.isEmpty()) return emptyList()

    val sorted = sessions.sortedBy { it.startTime }
    val result = mutableListOf<TimelineLayoutSession>()

    // 1. Group overlapping sessions into clusters
    val clusters = mutableListOf<MutableList<Session>>()
    var currentCluster = mutableListOf<Session>()
    var clusterEnd = -1L

    for (session in sorted) {
        if (currentCluster.isEmpty()) {
            currentCluster.add(session)
            clusterEnd = session.endTime
        } else if (session.startTime < clusterEnd) {
            currentCluster.add(session)
            clusterEnd = maxOf(clusterEnd, session.endTime)
        } else {
            clusters.add(currentCluster)
            currentCluster = mutableListOf(session)
            clusterEnd = session.endTime
        }
    }
    if (currentCluster.isNotEmpty()) {
        clusters.add(currentCluster)
    }

    // 2. In each cluster, assign column indices greedily
    for (cluster in clusters) {
        val colEndTimes = mutableListOf<Long>()
        val assignments = mutableListOf<Pair<Session, Int>>()

        for (session in cluster) {
            var placedCol = -1
            for (col in colEndTimes.indices) {
                if (colEndTimes[col] <= session.startTime) {
                    colEndTimes[col] = session.endTime
                    placedCol = col
                    break
                }
            }
            if (placedCol == -1) {
                colEndTimes.add(session.endTime)
                placedCol = colEndTimes.size - 1
            }
            assignments.add(Pair(session, placedCol))
        }

        val totalCols = colEndTimes.size
        for ((session, col) in assignments) {
            result.add(TimelineLayoutSession(session, col, totalCols))
        }
    }

    return result
}
