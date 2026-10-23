package com.example.ui.components

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.AlbumFolderEntity
import com.example.data.SingerFolderEntity
import com.example.data.SongEntity
import java.io.File
import kotlin.math.abs

private enum class HoldGestureOutcome {
    RELEASED_BEFORE_TIMEOUT,
    CANCELLED_BY_SCROLL_OR_MOVE,
    HELD_STATIONARY_OVER_ONE_SECOND
}

/**
 * Scroll-safe gesture modifier inspired by Google Files that triggers
 * [onLongPressMoreThanOneSecond] ONLY when the user presses and holds their finger
 * stationary on an item for more than 1 second (> 1000ms) without scrolling up, down,
 * or sideways. Any vertical or horizontal scroll/drag immediately cancels the hold
 * so scrolling never triggers multi-selection.
 */
fun Modifier.longPressMoreThanOneSecond(
    key: Any?,
    onClick: () -> Unit,
    onLongPressMoreThanOneSecond: () -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongPress by rememberUpdatedState(onLongPressMoreThanOneSecond)

    pointerInput(key) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (down.isConsumed) return@awaitEachGesture

            val startPos = down.position
            val pointerId = down.id
            // Strict movement threshold so any vertical or horizontal scroll cancels immediately
            val maxMoveSlopPx = (viewConfiguration.touchSlop * 0.75f).coerceAtLeast(8f)

            val outcome: HoldGestureOutcome = withTimeoutOrNull(1050L) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val tracked = event.changes.firstOrNull { it.id == pointerId }
                        ?: event.changes.firstOrNull()
                        ?: return@withTimeoutOrNull HoldGestureOutcome.CANCELLED_BY_SCROLL_OR_MOVE

                    // Multi-touch or consumed by a parent scroll container (LazyColumn / LazyGrid / LazyRow)
                    if (event.changes.size > 1 || event.changes.any { it.isConsumed }) {
                        return@withTimeoutOrNull HoldGestureOutcome.CANCELLED_BY_SCROLL_OR_MOVE
                    }

                    val dx = abs(tracked.position.x - startPos.x)
                    val dy = abs(tracked.position.y - startPos.y)
                    val distance = (tracked.position - startPos).getDistance()

                    // If user moves finger up, down, or sideways while scrolling, cancel immediately
                    if (dx > maxMoveSlopPx || dy > maxMoveSlopPx || distance > maxMoveSlopPx) {
                        return@withTimeoutOrNull HoldGestureOutcome.CANCELLED_BY_SCROLL_OR_MOVE
                    }

                    // Finger lifted cleanly before 1 second
                    if (!tracked.pressed) {
                        tracked.consume()
                        return@withTimeoutOrNull HoldGestureOutcome.RELEASED_BEFORE_TIMEOUT
                    }
                }
                @Suppress("UNREACHABLE_CODE")
                HoldGestureOutcome.CANCELLED_BY_SCROLL_OR_MOVE
            } ?: HoldGestureOutcome.HELD_STATIONARY_OVER_ONE_SECOND

            when (outcome) {
                HoldGestureOutcome.RELEASED_BEFORE_TIMEOUT -> {
                    currentOnClick()
                }
                HoldGestureOutcome.HELD_STATIONARY_OVER_ONE_SECOND -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnLongPress()
                    // Consume remaining pointer events until finger is lifted so onClick does not fire
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        event.changes.forEach { it.consume() }
                        if (event.changes.all { !it.pressed }) break
                    }
                }
                HoldGestureOutcome.CANCELLED_BY_SCROLL_OR_MOVE -> {
                    // Do nothing — let vertical/horizontal scrolling proceed undisturbed
                }
            }
        }
    }
}

/**
 * Google Files–style circular selection checkmark indicator for rows and folder cards.
 */
@Composable
fun GoogleFilesSelectionIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    unselectedBorderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
) {
    if (isSelected) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(size * 0.65f)
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.18f))
                .border(1.8.dp, unselectedBorderColor, CircleShape)
        )
    }
}

/**
 * Google Files–inspired contextual top selection bar & action bar.
 * Displays the number of selected items, total audio tracks/duration, Select All toggle,
 * and a prominent Share button.
 */
@Composable
fun MultiSelectionShareBar(
    selectedSongCount: Int,
    selectedAlbumCount: Int,
    selectedSingerCount: Int,
    resolvedTracks: List<SongEntity>,
    isAllSelectedInView: Boolean,
    onToggleSelectAllCurrentView: () -> Unit,
    onShareSelected: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSelectedItems = selectedSongCount + selectedAlbumCount + selectedSingerCount
    val isVisible = totalSelectedItems > 0

    if (isVisible) {
        BackHandler(onBack = onClearSelection)
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        val totalDurationMs = resolvedTracks.sumOf { it.durationMs }
        val detailsParts = buildList {
            if (selectedAlbumCount > 0) add("$selectedAlbumCount album${if (selectedAlbumCount == 1) "" else "s"}")
            if (selectedSingerCount > 0) add("$selectedSingerCount singer${if (selectedSingerCount == 1) "" else "s"}")
            add("${resolvedTracks.size} track${if (resolvedTracks.size == 1) "" else "s"}")
            if (totalDurationMs > 0L) add(formatDuration(totalDurationMs))
        }
        val subtitleText = detailsParts.joinToString(" • ")

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 12.dp,
            shadowElevation = 10.dp,
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                )
                .testTag("multi_selection_share_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onClearSelection,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("clear_selection_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Exit selection mode",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$totalSelectedItems selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onToggleSelectAllCurrentView,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("select_all_button")
                    ) {
                        Icon(
                            imageVector = if (isAllSelectedInView) {
                                Icons.Filled.CheckBox
                            } else {
                                Icons.Filled.CheckBoxOutlineBlank
                            },
                            contentDescription = if (isAllSelectedInView) "Deselect all" else "Select all",
                            tint = if (isAllSelectedInView) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }

                    Button(
                        onClick = onShareSelected,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("share_selected_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share selected",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

object AudioShareHelper {

    fun resolveSongsForSelection(
        allSongs: List<SongEntity>,
        allRecordings: List<SongEntity>,
        allAlbums: List<AlbumFolderEntity>,
        allSingers: List<SingerFolderEntity>,
        selectedSongIds: Set<Long>,
        selectedAlbumIds: Set<Long>,
        selectedSingerIds: Set<Long>
    ): List<SongEntity> {
        val combinedPool = (allSongs + allRecordings).distinctBy { it.id }
        val selectedAlbums = allAlbums.filter { it.id in selectedAlbumIds }
        val selectedSingers = allSingers.filter { it.id in selectedSingerIds }

        val resolved = LinkedHashMap<Long, SongEntity>()

        // 1. Directly selected songs
        for (song in combinedPool) {
            if (song.id in selectedSongIds) {
                resolved[song.id] = song
            }
        }

        // 2. Songs inside selected Album folders
        for (album in selectedAlbums) {
            val albumTracks = allSongs.filter {
                it.albumId == album.id || it.albumName.equals(album.title, ignoreCase = true)
            }
            for (track in albumTracks) {
                resolved[track.id] = track
            }
        }

        // 3. Songs inside selected Singer folders
        for (singer in selectedSingers) {
            val singerTracks = allSongs.filter {
                it.singerId == singer.id || it.singerName.equals(singer.name, ignoreCase = true)
            }
            for (track in singerTracks) {
                resolved[track.id] = track
            }
        }

        return resolved.values.toList()
    }

    fun shareSelection(
        context: Context,
        selectedSongs: List<SongEntity>,
        selectedAlbums: List<AlbumFolderEntity>,
        selectedSingers: List<SingerFolderEntity>
    ): Boolean {
        if (selectedSongs.isEmpty() && selectedAlbums.isEmpty() && selectedSingers.isEmpty()) {
            return false
        }

        val shareableUris = ArrayList<Uri>()
        val authority = "${context.packageName}.fileprovider"

        for (song in selectedSongs) {
            val uri = resolveShareableUri(context, authority, song)
            if (uri != null) {
                shareableUris.add(uri)
            }
        }

        val summaryBuilder = StringBuilder()
        if (selectedAlbums.isNotEmpty()) {
            summaryBuilder.append("Albums: ")
                .append(selectedAlbums.joinToString(", ") { "${it.title} (${it.singerName})" })
                .append("\n")
        }
        if (selectedSingers.isNotEmpty()) {
            summaryBuilder.append("Singers: ")
                .append(selectedSingers.joinToString(", ") { it.name })
                .append("\n")
        }
        if (selectedSongs.isNotEmpty()) {
            summaryBuilder.append("Tracks (${selectedSongs.size}):\n")
            selectedSongs.take(25).forEachIndexed { idx, song ->
                summaryBuilder.append("${idx + 1}. ${song.title} — ${song.singerName} (${song.albumName})\n")
            }
            if (selectedSongs.size > 25) {
                summaryBuilder.append("…and ${selectedSongs.size - 25} more tracks")
            }
        }
        val shareText = summaryBuilder.toString().trim()

        val sendIntent = when {
            shareableUris.size == 1 -> {
                Intent(Intent.ACTION_SEND).apply {
                    type = "audio/*"
                    putExtra(Intent.EXTRA_STREAM, shareableUris.first())
                    if (shareText.isNotEmpty()) {
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    clipData = ClipData.newRawUri("Audio Track", shareableUris.first())
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            shareableUris.size > 1 -> {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "audio/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, shareableUris)
                    if (shareText.isNotEmpty()) {
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    val clip = ClipData.newRawUri("Audio Tracks", shareableUris.first())
                    for (i in 1 until shareableUris.size) {
                        clip.addItem(ClipData.Item(shareableUris[i]))
                    }
                    clipData = clip
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            else -> {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText.ifEmpty { "Shared from Aura Music" })
                }
            }
        }

        val chooserTitle = when {
            selectedAlbums.size == 1 && selectedSingers.isEmpty() -> "Share Album: ${selectedAlbums.first().title}"
            selectedSingers.size == 1 && selectedAlbums.isEmpty() -> "Share Singer: ${selectedSingers.first().name}"
            selectedSongs.size == 1 -> "Share Song: ${selectedSongs.first().title}"
            else -> "Share ${selectedSongs.size} Audio Tracks"
        }

        return runCatching {
            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        }.getOrDefault(false)
    }

    private fun resolveShareableUri(
        context: Context,
        authority: String,
        song: SongEntity
    ): Uri? {
        return runCatching {
            val parsed = Uri.parse(song.uriString)
            when (parsed.scheme?.lowercase()) {
                "file" -> {
                    val path = parsed.path ?: return null
                    val file = File(path)
                    if (file.exists()) {
                        FileProvider.getUriForFile(context, authority, file)
                    } else null
                }
                "content" -> {
                    parsed
                }
                else -> {
                    val directFile = File(song.uriString)
                    if (directFile.exists()) {
                        FileProvider.getUriForFile(context, authority, directFile)
                    } else null
                }
            }
        }.getOrNull()
    }
}
