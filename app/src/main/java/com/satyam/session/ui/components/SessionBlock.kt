package com.satyam.session.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.satyam.session.data.model.ActivityBucket
import com.satyam.session.data.model.Session
import com.satyam.session.util.TimeUtils

/** Colored block rendered on the calendar timeline — height proportional to session duration. */
@Composable
fun SessionBlock(
    session: Session,
    bucket: ActivityBucket?,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val bucketColor = bucket?.let {
        Color(android.graphics.Color.parseColor(it.colorHex))
    } ?: MaterialTheme.colorScheme.primary

    val displayLabel = if (session.activityLabel.isNotBlank()) {
        session.activityLabel
    } else {
        bucket?.name ?: "Session"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.coerceAtLeast(24.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(bucketColor.copy(alpha = 0.85f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = displayLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (height > 32.dp) {
                Text(
                    text = TimeUtils.formatDurationShort(session.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}
