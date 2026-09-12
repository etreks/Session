package com.satyam.session.ui.stopwatch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.satyam.session.ui.components.BucketChip
import com.satyam.session.ui.components.CreateBucketDialog
import com.satyam.session.util.TimeUtils

/**
 * Stopwatch screen — inspired by Google Clock.
 * Large centered timer, bucket chip selector, play/stop FAB.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopwatchScreen(
    viewModel: StopwatchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.sessionJustSaved) {
        if (uiState.sessionJustSaved) {
            snackbarHostState.showSnackbar("Session saved ✓", duration = SnackbarDuration.Short)
            viewModel.dismissSavedMessage()
        }
    }

    if (uiState.showCreateBucket) {
        CreateBucketDialog(
            onDismiss = { viewModel.hideCreateBucketDialog() },
            onCreate = { name, color -> viewModel.createBucket(name, color) }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Session", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // ── Timer display ────────────────────────────────────────────
            AnimatedContent(
                targetState = uiState.elapsedMs,
                transitionSpec = {
                    fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                },
                label = "timer"
            ) { elapsed ->
                Text(
                    text = TimeUtils.formatDuration(elapsed),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp
                    ),
                    color = if (uiState.isRunning) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }

            // Pulsing "Recording" indicator
            if (uiState.isRunning) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = EaseInOutCubic),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )
                Text(
                    text = "● Recording",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Activity label input (hidden when running) ───────────────
            AnimatedVisibility(
                visible = !uiState.isRunning,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                OutlinedTextField(
                    value = uiState.activityLabel,
                    onValueChange = { viewModel.updateLabel(it) },
                    label = { Text("What are you working on?") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                )
            }

            // ── Bucket selector chips (hidden when running) ──────────────
            AnimatedVisibility(
                visible = !uiState.isRunning,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Activity",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = { viewModel.showCreateBucketDialog() }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add activity",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        items(uiState.buckets, key = { it.id }) { bucket ->
                            BucketChip(
                                bucket = bucket,
                                isSelected = bucket.id == uiState.selectedBucket?.id,
                                onClick = { viewModel.selectBucket(bucket) }
                            )
                        }
                    }
                }
            }

            // ── Running bucket indicator ─────────────────────────────────
            if (uiState.isRunning && uiState.selectedBucket != null) {
                val bucket = uiState.selectedBucket!!
                val label = if (uiState.activityLabel.isNotBlank()) {
                    "${bucket.name} · ${uiState.activityLabel}"
                } else bucket.name

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // ── Start / Stop FAB ─────────────────────────────────────────
            LargeFloatingActionButton(
                onClick = {
                    if (uiState.isRunning) viewModel.stopStopwatch()
                    else viewModel.startStopwatch()
                },
                containerColor = if (uiState.isRunning)
                    MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isRunning) Icons.Default.Stop
                    else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isRunning) "Stop" else "Start",
                    modifier = Modifier.size(36.dp),
                    tint = if (uiState.isRunning)
                        MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
