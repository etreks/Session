package com.satyam.session.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satyam.session.data.model.ActivityBucket
import com.satyam.session.data.model.Session
import com.satyam.session.util.TimeUtils

/**
 * Calendar Session Block — styled like Google Calendar event blocks.
 * Features:
 *  - Adaptive typography (single line for short events, multi-line for long events)
 *  - Left accent stripe matching bucket color
 *  - High contrast readability and subtle rounded border
 */
@Composable
fun SessionBlock(
    session: Session,
    bucket: ActivityBucket?,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val bucketColor = bucket?.let {
        try {
            Color(android.graphics.Color.parseColor(it.colorHex))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.primary
        }
    } ?: MaterialTheme.colorScheme.primary

    val displayLabel = session.activityLabel.ifBlank {
        bucket?.name ?: "Session"
    }

    val durationText = TimeUtils.formatDurationShort(session.durationMs)
    val timeRangeText = "${TimeUtils.formatTime(session.startTime)} – ${TimeUtils.formatTime(session.endTime)}"

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(bucketColor.copy(alpha = 0.90f))
            .border(
                width = 1.dp,
                color = bucketColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left vertical accent stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.6f))
            )

            // Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                if (height < 42.dp) {
                    // Single compact line for short sessions
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = displayLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            ),
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }
                } else {
                    // Multi-line detailed layout for standard sessions
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = displayLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "$durationText · $timeRangeText",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp
                            ),
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
