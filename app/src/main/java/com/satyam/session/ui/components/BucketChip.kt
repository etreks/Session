package com.satyam.session.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.satyam.session.data.model.ActivityBucket

/** Color-dotted chip for selecting an activity bucket — scrollable row in stopwatch screen. */
@Composable
fun BucketChip(
    bucket: ActivityBucket,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bucketColor = Color(android.graphics.Color.parseColor(bucket.colorHex))
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) bucketColor.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "chipColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) bucketColor
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        label = "borderColor"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            ),
        color = containerColor,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(bucketColor)
            )
            Text(
                text = bucket.name,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) bucketColor
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
