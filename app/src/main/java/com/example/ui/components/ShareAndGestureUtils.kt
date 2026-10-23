package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.AlbumFolderEntity
import com.example.data.SingerFolderEntity
import com.example.data.SongEntity
import com.example.ui.theme.AuraEmerald
import com.example.ui.theme.AuraIndigo
import java.io.File

sealed class ShareableTarget {
    data class SongTarget(val song: SongEntity) : ShareableTarget()
    data class AlbumTarget(val album: AlbumFolderEntity, val songs: List<SongEntity>) : ShareableTarget()
    data class SingerTarget(val singer: SingerFolderEntity, val songs: List<SongEntity>) : ShareableTarget()
}

val LocalSongDeleteHandler = compositionLocalOf<(SongEntity) -> Unit> { {} }
val LocalSongShareHandler = compositionLocalOf<(SongEntity) -> Unit> { {} }
val LocalShareTargetHandler = compositionLocalOf<(ShareableTarget) -> Unit> { {} }

const val LONG_PRESS_SHARE_DURATION_MS = 1200L

fun triggerLongPressHaptic(context: Context, view: View) {
    runCatching {
        view.performHapticFeedback(
            HapticFeedbackConstants.LONG_PRESS,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }
    runCatching {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(65L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(65L)
            }
        }
    }
}

/**
 * Detects a standard tap and a 1.2-second (1200ms) long-press with physical haptic vibration.
 */
fun Modifier.tapAndDelayedLongPress(
    key: Any? = Unit,
    longPressTimeoutMillis: Long = LONG_PRESS_SHARE_DURATION_MS,
    onTap: () -> Unit,
    onLongPressAfterThreshold: () -> Unit
): Modifier = composed {
    val context = LocalContext.current
    val view = LocalView.current
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPressAfterThreshold)

    this.pointerInput(key) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val upBeforeTimeout = withTimeoutOrNull(longPressTimeoutMillis) {
                waitForUpOrCancellation()
            }
            if (upBeforeTimeout != null) {
                upBeforeTimeout.consume()
                currentOnTap()
            } else {
                val stillPressed = currentEvent.changes.any { it.pressed && !it.isConsumed }
                if (stillPressed) {
                    triggerLongPressHaptic(context, view)
                    currentOnLongPress()
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            }
        }
    }
}

fun resolveShareableUri(context: Context, song: SongEntity): Uri? {
    return runCatching {
        val raw = song.uriString
        when {
            raw.startsWith("content://") -> Uri.parse(raw)
            raw.startsWith("file://") -> {
                val path = Uri.parse(raw).path ?: return@runCatching null
                val file = File(path)
                if (file.exists()) {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } else null
            }
            else -> {
                val file = File(raw)
                if (file.exists()) {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } else {
                    Uri.parse(raw)
                }
            }
        }
    }.getOrNull()
}

fun buildShareSummaryText(target: ShareableTarget): String {
    return when (target) {
        is ShareableTarget.SongTarget -> {
            val s = target.song
            "🎵 ${s.title}\n👤 Singer: ${s.singerName}\n💿 Album: ${s.albumName}\n⏱ Duration: ${formatDuration(s.durationMs)}\nShared from Aura Offline Music"
        }
        is ShareableTarget.AlbumTarget -> {
            val album = target.album
            val trackLines = target.songs.mapIndexed { idx, s ->
                "${idx + 1}. ${s.title} (${formatDuration(s.durationMs)})"
            }.joinToString("\n")
            "💿 Album: ${album.title}\n👤 Singer: ${album.singerName} (${album.releaseYear})\n🎶 ${target.songs.size} Songs:\n$trackLines\nShared from Aura Offline Music"
        }
        is ShareableTarget.SingerTarget -> {
            val singer = target.singer
            val trackLines = target.songs.mapIndexed { idx, s ->
                "${idx + 1}. ${s.title} • ${s.albumName} (${formatDuration(s.durationMs)})"
            }.joinToString("\n")
            "👤 Singer: ${singer.name} (${singer.genreTag})\n🎶 ${target.songs.size} Songs:\n$trackLines\nShared from Aura Offline Music"
        }
    }
}

fun launchShareAudioIntent(
    context: Context,
    target: ShareableTarget,
    onResultMessage: (String) -> Unit
) {
    val songsToShare = when (target) {
        is ShareableTarget.SongTarget -> listOf(target.song)
        is ShareableTarget.AlbumTarget -> target.songs
        is ShareableTarget.SingerTarget -> target.songs
    }
    val uris = ArrayList<Uri>(songsToShare.mapNotNull { resolveShareableUri(context, it) })
    val summaryText = buildShareSummaryText(target)
    val chooserTitle = when (target) {
        is ShareableTarget.SongTarget -> "Share Song: ${target.song.title}"
        is ShareableTarget.AlbumTarget -> "Share Album: ${target.album.title}"
        is ShareableTarget.SingerTarget -> "Share Singer: ${target.singer.name}"
    }

    runCatching {
        val shareIntent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uris.first())
                putExtra(Intent.EXTRA_TEXT, summaryText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else if (uris.size > 1) {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "audio/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                putExtra(Intent.EXTRA_TEXT, summaryText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, summaryText)
            }
        }

        val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }.onFailure {
        onResultMessage("Unable to open external share chooser on this device")
    }
}

fun launchShareTextIntent(
    context: Context,
    target: ShareableTarget,
    onResultMessage: (String) -> Unit
) {
    val summaryText = buildShareSummaryText(target)
    val chooserTitle = when (target) {
        is ShareableTarget.SongTarget -> "Share Track Info: ${target.song.title}"
        is ShareableTarget.AlbumTarget -> "Share Album Tracklist: ${target.album.title}"
        is ShareableTarget.SingerTarget -> "Share Singer Catalog: ${target.singer.name}"
    }
    runCatching {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, summaryText)
        }
        val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }.onFailure {
        onResultMessage("Unable to open text share sheet")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareTargetOptionsSheet(
    target: ShareableTarget,
    onDismiss: () -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("share_options_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (target) {
                    is ShareableTarget.SongTarget -> {
                        AlbumArtworkView(
                            colorHex = target.song.colorHex,
                            title = target.song.albumName,
                            artworkPath = target.song.artworkPath,
                            cornerRadius = 14.dp,
                            modifier = Modifier.size(58.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SHARE SONG",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = target.song.title,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${target.song.singerName} • ${target.song.albumName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    is ShareableTarget.AlbumTarget -> {
                        val art = target.album.artworkPath
                            ?: target.songs.firstOrNull { !it.artworkPath.isNullOrBlank() }?.artworkPath
                        AlbumArtworkView(
                            colorHex = target.album.coverColorHex,
                            title = target.album.title,
                            artworkPath = art,
                            cornerRadius = 14.dp,
                            modifier = Modifier.size(58.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SHARE ALBUM (${target.songs.size} SONGS)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = target.album.title,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${target.album.singerName} • ${target.album.releaseYear}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    is ShareableTarget.SingerTarget -> {
                        val art = target.singer.artworkPath
                            ?: target.songs.firstOrNull { !it.artworkPath.isNullOrBlank() }?.artworkPath
                        SingerAvatarView(
                            colorHex = target.singer.avatarColorHex,
                            singerName = target.singer.name,
                            artworkPath = art,
                            modifier = Modifier.size(58.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SHARE SINGER (${target.songs.size} SONGS)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = target.singer.name,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = target.singer.genreTag,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(14.dp))

            val audioOptionTitle = when (target) {
                is ShareableTarget.SongTarget -> "Share Audio File"
                is ShareableTarget.AlbumTarget -> "Share All Album Audio Files (${target.songs.size})"
                is ShareableTarget.SingerTarget -> "Share All Singer Audio Files (${target.songs.size})"
            }
            val audioOptionSubtitle = when (target) {
                is ShareableTarget.SongTarget -> "Send the audio track file directly to another app or contact"
                is ShareableTarget.AlbumTarget -> "Send all ${target.songs.size} audio tracks in ${target.album.title}"
                is ShareableTarget.SingerTarget -> "Send all ${target.songs.size} audio tracks by ${target.singer.name}"
            }

            ShareActionRowCard(
                icon = Icons.Filled.AudioFile,
                title = audioOptionTitle,
                subtitle = audioOptionSubtitle,
                accentColor = MaterialTheme.colorScheme.primary,
                onClick = {
                    launchShareAudioIntent(context, target, onStatusMessage)
                    onDismiss()
                },
                modifier = Modifier.testTag("share_audio_file_button")
            )

            Spacer(modifier = Modifier.height(10.dp))

            val textOptionTitle = when (target) {
                is ShareableTarget.SongTarget -> "Share Song Info & Details"
                is ShareableTarget.AlbumTarget -> "Share Album Tracklist & Info"
                is ShareableTarget.SingerTarget -> "Share Singer Tracklist & Profile"
            }

            ShareActionRowCard(
                icon = Icons.Filled.Share,
                title = textOptionTitle,
                subtitle = "Share formatted title, artist, album, and duration details",
                accentColor = AuraEmerald,
                onClick = {
                    launchShareTextIntent(context, target, onStatusMessage)
                    onDismiss()
                },
                modifier = Modifier.testTag("share_summary_text_button")
            )

            Spacer(modifier = Modifier.height(10.dp))

            ShareActionRowCard(
                icon = Icons.Filled.ContentCopy,
                title = "Copy Share Details",
                subtitle = "Copy formatted details to your clipboard",
                accentColor = AuraIndigo,
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val summary = buildShareSummaryText(target)
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Aura Music Share", summary))
                    onStatusMessage("Copied share details to clipboard")
                    onDismiss()
                },
                modifier = Modifier.testTag("copy_share_info_button")
            )
        }
    }
}

@Composable
private fun ShareActionRowCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 0.8.dp,
                color = accentColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
