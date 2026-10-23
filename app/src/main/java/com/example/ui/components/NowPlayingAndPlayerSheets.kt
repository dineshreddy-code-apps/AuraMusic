package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.SongEntity
import com.example.player.EqualizerPreset
import com.example.player.PlaybackState
import com.example.player.RepeatMode
import com.example.player.SavedCustomTuning
import com.example.ui.theme.AuraEmerald
import com.example.ui.theme.AuraLightBlue
import com.example.ui.theme.LocalIsDarkTheme
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun PersistentMiniPlayer(
    playback: PlaybackState,
    onExpandNowPlaying: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleLike: (SongEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val song = playback.currentSong ?: return
    val progress = (playback.currentPositionMs.toFloat() / playback.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
    var accumulatedDragY by remember { mutableStateOf(0f) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .border(
                width = 0.8.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                shape = RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { accumulatedDragY = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDragY += dragAmount
                        // Open Now Playing when swiped up (or swiped vertically past threshold)
                        if (accumulatedDragY < -24f) {
                            accumulatedDragY = 0f
                            onExpandNowPlaying()
                        }
                    },
                    onDragEnd = {
                        if (kotlin.math.abs(accumulatedDragY) > 20f && accumulatedDragY < 0f) {
                            onExpandNowPlaying()
                        }
                        accumulatedDragY = 0f
                    }
                )
            }
            .clickable(onClick = onExpandNowPlaying)
            .testTag("mini_player_bar")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Subtle top pill handle hinting swipe-up gesture
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtworkView(
                    colorHex = song.colorHex,
                    title = song.albumName,
                    cornerRadius = 10.dp,
                    isPlayingIndicator = playback.isPlaying,
                    artworkPath = song.artworkPath,
                    modifier = Modifier.size(42.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.singerName} • ${song.albumName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { onToggleLike(song) },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("mini_player_like_button")
                ) {
                    Icon(
                        imageVector = if (song.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like current song",
                        tint = if (song.isLiked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                val isDark = LocalIsDarkTheme.current
                val skipTrackTint = if (isDark) AuraLightBlue else MaterialTheme.colorScheme.onSurface

                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("mini_player_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous track",
                        tint = skipTrackTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("mini_player_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playback.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("mini_player_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next track",
                        tint = skipTrackTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )
        }
    }
}

@Composable
fun MinimalistWaveformVisualizer(
    amplitudes: List<Float>,
    progressFraction: Float,
    isPlaying: Boolean,
    activeColor: Color,
    accentGlowColor: Color,
    inactiveColor: Color,
    onScrubFraction: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_phase")
    val livePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing)
        ),
        label = "live_phase"
    )

    val bars = if (amplitudes.isEmpty()) List(36) { 0.25f } else amplitudes

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (size.width > 0) {
                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onScrubFraction(fraction)
                    }
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    if (size.width > 0) {
                        val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onScrubFraction(fraction)
                    }
                }
            }
    ) {
        val count = bars.size
        val totalWidth = size.width
        val centerY = size.height / 2f
        val slotWidth = totalWidth / count
        val barWidth = (slotWidth * 0.56f).coerceAtLeast(2.dp.toPx())
        val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

        bars.forEachIndexed { index, baseAmp ->
            val barFraction = index.toFloat() / (count - 1).coerceAtLeast(1)
            val isPlayed = barFraction <= progressFraction
            val isPlayheadBar = kotlin.math.abs(barFraction - progressFraction) < (1f / count)

            val dynamicModulation = if (isPlaying) {
                val harmonic = 0.82f + 0.18f * sin(livePhase + index * 0.48f)
                (baseAmp * harmonic).coerceIn(0.10f, 1.0f)
            } else {
                (baseAmp * 0.55f).coerceIn(0.10f, 0.72f)
            }

            val barHeight = (size.height * dynamicModulation).coerceAtLeast(6.dp.toPx())
            val left = index * slotWidth + (slotWidth - barWidth) / 2f
            val top = centerY - barHeight / 2f

            if (isPlayed) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentGlowColor.copy(alpha = 0.95f),
                            activeColor,
                            accentGlowColor.copy(alpha = 0.90f)
                        ),
                        startY = top,
                        endY = top + barHeight
                    ),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = cornerRadius
                )
            } else {
                drawRoundRect(
                    color = inactiveColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = cornerRadius
                )
            }

            if (isPlayheadBar) {
                drawCircle(
                    color = activeColor.copy(alpha = 0.28f),
                    radius = barWidth * 1.4f,
                    center = Offset(left + barWidth / 2f, centerY)
                )
            }
        }
    }
}

@Composable
fun FullScreenNowPlayingView(
    playback: PlaybackState,
    onCollapse: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: (SongEntity) -> Unit,
    onOpenSongOptions: (SongEntity) -> Unit,
    onPlayQueueIndex: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onSetEqualizerEnabled: (Boolean) -> Unit,
    onSelectEqualizerPreset: (EqualizerPreset) -> Unit,
    onUpdateBandGain: (Int, Float) -> Unit,
    onUpdateBassBoost: (Int) -> Unit,
    onSaveCustomTuning: (String) -> Unit = {},
    onApplySavedCustomTuning: (SavedCustomTuning) -> Unit = {},
    onDeleteSavedCustomTuning: (String) -> Unit = {},
    onResetCustomTuningToFlat: () -> Unit = {},
    onSetSleepTimerMinutes: (Int?) -> Unit,
    onSetStopAtEndOfTrack: (Boolean) -> Unit
) {
    BackHandler(onBack = onCollapse)

    val song = playback.currentSong ?: return
    val accentColor = Color(song.colorHex)
    var showQueueSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }

    var swipeDownOffsetPx by remember { mutableStateOf(0f) }
    val animatedOffsetPx by animateFloatAsState(
        targetValue = swipeDownOffsetPx,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "swipe_down_offset"
    )

    val artworkScale by animateFloatAsState(
        targetValue = if (playback.isPlaying) 1.0f else 0.90f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 320f),
        label = "artwork_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset {
                androidx.compose.ui.unit.IntOffset(0, animatedOffsetPx.toInt().coerceAtLeast(0))
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { swipeDownOffsetPx = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        // Track downward drag to minimize Now Playing
                        val next = (swipeDownOffsetPx + dragAmount).coerceAtLeast(0f)
                        if (next > 0f) {
                            change.consume()
                            swipeDownOffsetPx = next
                        }
                    },
                    onDragEnd = {
                        if (swipeDownOffsetPx > 140f) {
                            swipeDownOffsetPx = 0f
                            onCollapse()
                        } else {
                            swipeDownOffsetPx = 0f
                        }
                    },
                    onDragCancel = {
                        swipeDownOffsetPx = 0f
                    }
                )
            }
            .background(MaterialTheme.colorScheme.background)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.34f),
                        accentColor.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("full_now_playing_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .align(Alignment.Center)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top swipe-down grab handle pill
            Box(
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
            // Top bar with collapse chevron, playing context, and more options
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.testTag("collapse_now_playing_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Minimize player",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYING FROM ALBUM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = song.albumName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { onOpenSongOptions(song) },
                    modifier = Modifier.testTag("now_playing_options_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = "Track options",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Elevated Album Artwork
            AlbumArtworkView(
                colorHex = song.colorHex,
                title = song.albumName,
                cornerRadius = 24.dp,
                isPlayingIndicator = false,
                artworkPath = song.artworkPath,
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .aspectRatio(1f)
                    .scale(artworkScale)
                    .shadow(24.dp, RoundedCornerShape(24.dp))
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Track Title, Singer, and Favorite Heart
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${song.singerName} • ${song.albumName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { onToggleLike(song) },
                    modifier = Modifier.testTag("now_playing_like_button")
                ) {
                    Icon(
                        imageVector = if (song.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (song.isLiked) "Unlike song" else "Like song",
                        tint = if (song.isLiked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Interactive Progress Slider & Timestamps
            val durationMs = playback.durationMs.coerceAtLeast(1L)
            val currentPosMs = playback.currentPositionMs.coerceIn(0L, durationMs)
            val remainingMs = (durationMs - currentPosMs).coerceAtLeast(0L)

            Slider(
                value = currentPosMs.toFloat(),
                onValueChange = { onSeekTo(it.toLong()) },
                valueRange = 0f..durationMs.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("now_playing_seek_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(currentPosMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "-${formatDuration(remainingMs)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Transport Controls (Shuffle, Previous, Play/Pause, Next, Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.testTag("now_playing_shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Toggle shuffle",
                        tint = if (playback.shuffleEnabled) {
                            AuraEmerald
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                val isDark = LocalIsDarkTheme.current
                val skipTrackTint = if (isDark) AuraLightBlue else MaterialTheme.colorScheme.onSurface

                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("now_playing_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous track",
                        tint = skipTrackTint,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Surface(
                    onClick = onTogglePlayPause,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("now_playing_play_pause_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playback.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("now_playing_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next track",
                        tint = skipTrackTint,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier.testTag("now_playing_repeat_button")
                ) {
                    val repeatIcon = if (playback.repeatMode == RepeatMode.ONE) {
                        Icons.Filled.RepeatOne
                    } else {
                        Icons.Filled.Repeat
                    }
                    val repeatTint = if (playback.repeatMode != RepeatMode.OFF) {
                        AuraEmerald
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat mode: ${playback.repeatMode.name}",
                        tint = repeatTint
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Bottom Studio Utility Bar: Equalizer, Sleep Timer, Up Next Queue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val eqChipLabel = if (playback.selectedPreset == EqualizerPreset.CUSTOM && !playback.activeCustomTuningName.isNullOrBlank()) {
                    playback.activeCustomTuningName
                } else {
                    playback.selectedPreset.label
                }
                AssistChip(
                    onClick = { showEqualizerSheet = true },
                    label = {
                        Text(
                            text = eqChipLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Equalizer,
                            contentDescription = null,
                            tint = if (playback.equalizerEnabled) AuraEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("open_equalizer_button")
                )

                val timerBadge = when {
                    playback.stopAtEndOfTrack -> "End of Song"
                    playback.sleepTimerRemainingMs != null -> formatDuration(playback.sleepTimerRemainingMs)
                    else -> "Sleep Timer"
                }
                AssistChip(
                    onClick = { showSleepTimerSheet = true },
                    label = { Text(timerBadge) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Bedtime,
                            contentDescription = null,
                            tint = if (playback.sleepTimerRemainingMs != null || playback.stopAtEndOfTrack) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("open_sleep_timer_button")
                )

                AssistChip(
                    onClick = { showQueueSheet = true },
                    label = { Text("Up Next (${playback.queue.size})") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("open_queue_button")
                )
            }
        }
    }

    if (showQueueSheet) {
        UpNextQueueBottomSheet(
            playback = playback,
            onDismiss = { showQueueSheet = false },
            onPlayQueueIndex = onPlayQueueIndex,
            onMoveQueueItem = onMoveQueueItem,
            onRemoveQueueItem = onRemoveQueueItem,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeat = onCycleRepeat
        )
    }

    if (showEqualizerSheet) {
        EqualizerBottomSheet(
            playback = playback,
            onDismiss = { showEqualizerSheet = false },
            onSetEnabled = onSetEqualizerEnabled,
            onSelectPreset = onSelectEqualizerPreset,
            onUpdateBandGain = onUpdateBandGain,
            onUpdateBassBoost = onUpdateBassBoost,
            onSaveCustomTuning = onSaveCustomTuning,
            onApplySavedCustomTuning = onApplySavedCustomTuning,
            onDeleteSavedCustomTuning = onDeleteSavedCustomTuning,
            onResetCustomTuningToFlat = onResetCustomTuningToFlat
        )
    }

    if (showSleepTimerSheet) {
        SleepTimerBottomSheet(
            playback = playback,
            onDismiss = { showSleepTimerSheet = false },
            onSelectMinutes = onSetSleepTimerMinutes,
            onSetStopAtEndOfTrack = onSetStopAtEndOfTrack
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpNextQueueBottomSheet(
    playback: PlaybackState,
    onDismiss: () -> Unit,
    onPlayQueueIndex: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Up Next Queue",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "${playback.queue.size} tracks in active queue",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = playback.shuffleEnabled,
                        onClick = onToggleShuffle,
                        label = { Text("Shuffle") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    FilterChip(
                        selected = playback.repeatMode != RepeatMode.OFF,
                        onClick = onCycleRepeat,
                        label = { Text("Repeat: ${playback.repeatMode.name}") },
                        modifier = Modifier.testTag("queue_repeat_chip")
                    )
                }
            }

            if (playback.repeatMode == RepeatMode.OFF && !playback.autoContinuationLabel.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("continuous_queue_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = playback.autoContinuationLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(playback.queue, key = { idx, item -> "${item.id}_$idx" }) { index, item ->
                    val isCurrent = index == playback.currentQueueIndex
                    Surface(
                        color = if (isCurrent) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayQueueIndex(index) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AlbumArtworkView(
                                colorHex = item.colorHex,
                                title = item.albumName,
                                cornerRadius = 8.dp,
                                isPlayingIndicator = isCurrent && playback.isPlaying,
                                artworkPath = item.artworkPath,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.singerName} • ${item.albumName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (index > 0) {
                                IconButton(
                                    onClick = { onMoveQueueItem(index, index - 1) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowUpward,
                                        contentDescription = "Move up",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (index < playback.queue.lastIndex) {
                                IconButton(
                                    onClick = { onMoveQueueItem(index, index + 1) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDownward,
                                        contentDescription = "Move down",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (playback.queue.size > 1) {
                                IconButton(
                                    onClick = { onRemoveQueueItem(index) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove from queue",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EqualizerBottomSheet(
    playback: PlaybackState,
    onDismiss: () -> Unit,
    onSetEnabled: (Boolean) -> Unit,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onUpdateBandGain: (Int, Float) -> Unit,
    onUpdateBassBoost: (Int) -> Unit,
    onSaveCustomTuning: (String) -> Unit = {},
    onApplySavedCustomTuning: (SavedCustomTuning) -> Unit = {},
    onDeleteSavedCustomTuning: (String) -> Unit = {},
    onResetCustomTuningToFlat: () -> Unit = {}
) {
    val bandLabels = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
    val primaryColor = MaterialTheme.colorScheme.primary
    val focusManager = LocalFocusManager.current
    var showCustomTuningStudio by remember { mutableStateOf(false) }
    var customTuningName by remember(playback.activeCustomTuningName) {
        mutableStateOf(playback.activeCustomTuningName ?: "")
    }
    var justSavedBanner by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Equalizer & Studio FX",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    val subtitleText = if (playback.selectedPreset == EqualizerPreset.CUSTOM && !playback.activeCustomTuningName.isNullOrBlank()) {
                        "Active Custom Tuning: ${playback.activeCustomTuningName} • Locked across songs"
                    } else {
                        playback.selectedPreset.subtitle
                    }
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = playback.equalizerEnabled,
                    onCheckedChange = onSetEnabled,
                    modifier = Modifier.testTag("equalizer_enable_switch")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Acoustic Frequency Response Curve Visualization
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    val gains = playback.customBandGainsDb
                    if (gains.isEmpty()) return@Canvas
                    val stepX = size.width / (gains.size - 1).coerceAtLeast(1)
                    val midY = size.height / 2f
                    val path = Path()

                    gains.forEachIndexed { idx, db ->
                        val x = idx * stepX
                        val normalized = (db / 12f).coerceIn(-1f, 1f)
                        val y = midY - normalized * (midY * 0.8f)
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        drawCircle(
                            color = primaryColor,
                            radius = 5.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                    drawPath(
                        path = path,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Acoustic Presets",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EqualizerPreset.entries.forEach { preset ->
                    val isSelected = playback.selectedPreset == preset &&
                        (preset != EqualizerPreset.CUSTOM || playback.activeCustomTuningId == null)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            justSavedBanner = null
                            onSelectPreset(preset)
                        },
                        label = { Text(preset.label) },
                        enabled = playback.equalizerEnabled,
                        modifier = Modifier.testTag("eq_preset_${preset.name}")
                    )
                }
            }

            // Saved Custom Tunings Section
            if (playback.savedCustomTunings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.BookmarkAdded,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Saved Custom Tunings (${playback.savedCustomTunings.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    playback.savedCustomTunings.forEach { tuning ->
                        val isActiveTuning = playback.selectedPreset == EqualizerPreset.CUSTOM &&
                            playback.activeCustomTuningId == tuning.id
                        FilterChip(
                            selected = isActiveTuning,
                            onClick = {
                                justSavedBanner = null
                                customTuningName = tuning.name
                                onApplySavedCustomTuning(tuning)
                            },
                            enabled = playback.equalizerEnabled,
                            label = {
                                Text(
                                    text = tuning.name,
                                    fontWeight = if (isActiveTuning) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (isActiveTuning) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Delete ${tuning.name}",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onDeleteSavedCustomTuning(tuning.id) }
                                        .testTag("delete_saved_tuning_${tuning.id}")
                                )
                            },
                            modifier = Modifier.testTag("saved_custom_tuning_chip_${tuning.id}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Top header bar directly above Hz controls with small button to Save Custom Tunings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Hz Controls",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = {
                            justSavedBanner = null
                            onResetCustomTuningToFlat()
                        },
                        enabled = playback.equalizerEnabled,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("reset_custom_tuning_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset 0 dB", style = MaterialTheme.typography.labelSmall)
                    }
                }

                FilledTonalButton(
                    onClick = {
                        justSavedBanner = null
                        if (!showCustomTuningStudio && customTuningName.isBlank()) {
                            customTuningName = playback.activeCustomTuningName ?: ""
                        }
                        showCustomTuningStudio = !showCustomTuningStudio
                    },
                    enabled = playback.equalizerEnabled,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("open_custom_tuning_studio_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save Custom Tuning",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            justSavedBanner?.let { bannerMsg ->
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = AuraEmerald,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = bannerMsg,
                        style = MaterialTheme.typography.labelSmall,
                        color = AuraEmerald,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Expandable Custom Tuning name-entry prompt that auto-exits immediately once saved
            AnimatedVisibility(visible = showCustomTuningStudio) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        OutlinedTextField(
                            value = customTuningName,
                            onValueChange = { customTuningName = it },
                            enabled = playback.equalizerEnabled,
                            singleLine = true,
                            label = { Text("Custom Tuning Name") },
                            placeholder = { Text("e.g., Car Bass, Studio Headphones, Warm Vocal") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_tuning_name_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    showCustomTuningStudio = false
                                }
                            ) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val targetName = customTuningName.trim().ifBlank {
                                        playback.activeCustomTuningName
                                            ?: "Custom Studio #${playback.savedCustomTunings.size + 1}"
                                    }
                                    customTuningName = targetName
                                    onSaveCustomTuning(targetName)
                                    focusManager.clearFocus()
                                    // Auto-exit from Custom Tuning Studio immediately once saved
                                    showCustomTuningStudio = false
                                    justSavedBanner = "Saved \"$targetName\" to Custom Tunings"
                                },
                                enabled = playback.equalizerEnabled,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("save_custom_tuning_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Save",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5-band interactive sliders
            playback.customBandGainsDb.forEachIndexed { idx, gainDb ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = bandLabels.getOrElse(idx) { "Band ${idx + 1}" },
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(52.dp)
                    )
                    Slider(
                        value = gainDb,
                        onValueChange = {
                            justSavedBanner = null
                            onUpdateBandGain(idx, it)
                        },
                        valueRange = -12f..12f,
                        enabled = playback.equalizerEnabled,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("eq_band_slider_$idx")
                    )
                    Text(
                        text = "${if (gainDb >= 0f) "+" else ""}${gainDb.toInt()} dB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(46.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bass Boost Strength Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sub Bass",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(52.dp)
                )
                Slider(
                    value = playback.bassBoostStrength.toFloat(),
                    onValueChange = {
                        justSavedBanner = null
                        onUpdateBassBoost(it.toInt())
                    },
                    valueRange = 0f..1000f,
                    enabled = playback.equalizerEnabled,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("eq_bass_boost_slider")
                )
                Text(
                    text = "${playback.bassBoostStrength / 10}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(46.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SleepTimerBottomSheet(
    playback: PlaybackState,
    onDismiss: () -> Unit,
    onSelectMinutes: (Int?) -> Unit,
    onSetStopAtEndOfTrack: (Boolean) -> Unit
) {
    val timerOptions = listOf(15, 30, 45, 60)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Sleep Timer",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Automatically pause offline playback after a set duration.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                timerOptions.forEach { mins ->
                    AssistChip(
                        onClick = {
                            onSelectMinutes(mins)
                            onDismiss()
                        },
                        label = { Text("$mins Minutes") },
                        modifier = Modifier.testTag("sleep_timer_${mins}_min")
                    )
                }

                FilterChip(
                    selected = playback.stopAtEndOfTrack,
                    onClick = {
                        onSetStopAtEndOfTrack(!playback.stopAtEndOfTrack)
                        onDismiss()
                    },
                    label = { Text("End of Current Track") },
                    modifier = Modifier.testTag("sleep_timer_end_of_track")
                )

                if (playback.sleepTimerRemainingMs != null || playback.stopAtEndOfTrack) {
                    AssistChip(
                        onClick = {
                            onSelectMinutes(null)
                            onDismiss()
                        },
                        label = { Text("Turn Off Timer") },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("sleep_timer_off")
                    )
                }
            }
        }
    }
}
