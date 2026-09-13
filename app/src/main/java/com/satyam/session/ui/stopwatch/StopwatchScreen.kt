package com.satyam.session.ui.stopwatch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
 * Focus Screen — integrates Google Clock-inspired Stopwatch & Countdown Timer.
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
            snackbarHostState.showSnackbar("Focus session saved ✓", duration = SnackbarDuration.Short)
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
                title = {
                    Text(
                        text = if (uiState.isTimerMode) "Focus Timer" else "Stopwatch",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
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
            // ── Mode Switcher (Stopwatch ⟷ Timer) ─────────────────────
            if (!uiState.isRunning) {
                FocusModeSwitcher(
                    isTimerMode = uiState.isTimerMode,
                    onModeSelected = { viewModel.setMode(it) },
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            if (uiState.isTimerMode) {
                // ── TIMER MODE ─────────────────────────────────────────
                if (uiState.isRunning) {
                    // Running Countdown with Circular Progress Ring
                    TimerRunningContent(
                        uiState = uiState,
                        onPause = { viewModel.pauseTimer() },
                        onResume = { viewModel.resumeTimer() },
                        onCancel = { viewModel.cancelOrStopTimer() },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    // Timer Setup (Numpad + Presets + Bucket chips)
                    TimerSetupContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // ── STOPWATCH MODE ─────────────────────────────────────
                StopwatchContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Top Mode Switcher Pill (⏱️ Stopwatch | ⏳ Timer)
 */
@Composable
private fun FocusModeSwitcher(
    isTimerMode: Boolean,
    onModeSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stopwatch Tab
            val stopwatchBg by animateColorAsState(
                targetValue = if (!isTimerMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                animationSpec = tween(250),
                label = "stopwatchBg"
            )
            val stopwatchText by animateColorAsState(
                targetValue = if (!isTimerMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(250),
                label = "stopwatchText"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(stopwatchBg)
                    .clickable { onModeSelected(false) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Stopwatch",
                        modifier = Modifier.size(18.dp),
                        tint = stopwatchText
                    )
                    Text(
                        text = "Stopwatch",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = stopwatchText
                    )
                }
            }

            // Timer Tab
            val timerBg by animateColorAsState(
                targetValue = if (isTimerMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                animationSpec = tween(250),
                label = "timerBg"
            )
            val timerText by animateColorAsState(
                targetValue = if (isTimerMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(250),
                label = "timerText"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(timerBg)
                    .clickable { onModeSelected(true) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassBottom,
                        contentDescription = "Timer",
                        modifier = Modifier.size(18.dp),
                        tint = timerText
                    )
                    Text(
                        text = "Timer",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = timerText
                    )
                }
            }
        }
    }
}

/**
 * Google Clock-style Timer Setup Screen (Digits + Presets + Activity Bucket + Numpad)
 */
@Composable
private fun TimerSetupContent(
    uiState: StopwatchUiState,
    viewModel: StopwatchViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // 1. Digital Time Input Display (HH:MM:SS)
        TimerDigitsDisplay(
            digits = uiState.timerDigits,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Quick Preset Focus Pills (5m, 15m, 25m, 45m, 60m)
        TimerPresetChips(
            selectedMinutes = uiState.selectedPresetMinutes,
            onSelectPreset = { viewModel.setPresetDuration(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Activity Bucket Selector
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Activity Bucket",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
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
                modifier = Modifier.padding(bottom = 8.dp)
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

        // Optional Activity Label
        OutlinedTextField(
            value = uiState.activityLabel,
            onValueChange = { viewModel.updateLabel(it) },
            label = { Text("What are you focusing on? (Optional)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Touch Numpad (1-9, 00, 0, ⌫)
        TimerNumpad(
            onDigitClick = { viewModel.inputDigit(it) },
            onDoubleZeroClick = { viewModel.inputDoubleZero() },
            onBackspaceClick = { viewModel.backspaceDigit() }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Start Focus Timer Button
        val canStart = uiState.targetDurationMs > 0 && uiState.selectedBucket != null
        LargeFloatingActionButton(
            onClick = { if (canStart) viewModel.startTimer() },
            containerColor = if (canStart) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Start Timer",
                modifier = Modifier.size(36.dp),
                tint = if (canStart) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
    }
}

/**
 * Running Countdown Timer with Circular Progress Ring (Matches Google Clock Screenshot 2)
 */
@Composable
private fun TimerRunningContent(
    uiState: StopwatchUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bucketColor = uiState.selectedBucket?.let {
        try {
            Color(android.graphics.Color.parseColor(it.colorHex))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.primary
        }
    } ?: MaterialTheme.colorScheme.primary

    val totalDuration = uiState.targetDurationMs.coerceAtLeast(1L)
    val remaining = uiState.remainingMs.coerceAtLeast(0L)
    val progress = (remaining.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(150),
        label = "timerRing"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // ── Circular Progress Ring with Remaining Time ───────────────
        Box(
            modifier = Modifier
                .size(280.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Circular Arc Ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 10.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)

                // Background track circle
                drawCircle(
                    color = bucketColor.copy(alpha = 0.15f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )

                // Active sweeping countdown arc
                drawArc(
                    color = bucketColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Inside Ring: Remaining Time Digits
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = TimeUtils.formatDuration(remaining),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        fontFeatureSettings = "tnum"
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Pause / Play quick indicator
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { if (uiState.isPaused) onResume() else onPause() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (uiState.isPaused) "Resume" else "Pause",
                            tint = bucketColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Activity Tag Badge
        uiState.selectedBucket?.let { bucket ->
            val labelText = if (uiState.activityLabel.isNotBlank()) {
                "${bucket.name} · ${uiState.activityLabel}"
            } else bucket.name

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(bucketColor)
                    )
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Controls: Pause/Resume FAB + Cancel Button ───────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel Button
            SmallFloatingActionButton(
                onClick = onCancel,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel timer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Main Pause / Resume FAB
            LargeFloatingActionButton(
                onClick = { if (uiState.isPaused) onResume() else onPause() },
                containerColor = bucketColor.copy(alpha = 0.25f)
            ) {
                Icon(
                    imageVector = if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (uiState.isPaused) "Resume" else "Pause",
                    tint = bucketColor,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

/**
 * Open-ended Stopwatch Content
 */
@Composable
private fun StopwatchContent(
    uiState: StopwatchUiState,
    viewModel: StopwatchViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // ── Stopwatch Timer display (monospaced tabular figures) ──────
        Text(
            text = TimeUtils.formatDuration(uiState.elapsedMs),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 68.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp,
                fontFeatureSettings = "tnum"
            ),
            color = if (uiState.isRunning) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 24.dp)
        )

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
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
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
