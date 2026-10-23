package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.RecordingCategory
import com.example.data.SongEntity
import com.example.ui.FolderSortOrder
import com.example.ui.MusicUiState
import com.example.ui.components.SongRowItem
import com.example.ui.components.formatDuration

@Composable
fun RecordingsSectionScreen(
    uiState: MusicUiState,
    onSelectCategory: (RecordingCategory?) -> Unit,
    onSortOrderChange: (FolderSortOrder) -> Unit,
    onRequestDeviceScan: () -> Unit,
    onImportToCategory: (RecordingCategory) -> Unit,
    onPlayRecording: (SongEntity, List<SongEntity>) -> Unit,
    onPlayAllInFolder: (List<SongEntity>, Boolean) -> Unit,
    onToggleLike: (SongEntity) -> Unit,
    onOpenRecordingOptions: (SongEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCategory = uiState.selectedRecordingCategory
    if (activeCategory != null) {
        val folderRecordings = uiState.recordings.filter {
            it.recordingCategory == activeCategory.id
        }
        RecordingFolderDetailView(
            category = activeCategory,
            folderRecordings = folderRecordings,
            sortOrder = uiState.recordingSortOrder,
            sortReversed = uiState.recordingSortReversed,
            currentSongId = uiState.playback.currentSong?.id,
            isPlaying = uiState.playback.isPlaying,
            onBack = { onSelectCategory(null) },
            onSortOrderChange = onSortOrderChange,
            onImportIntoFolder = { onImportToCategory(activeCategory) },
            onPlayRecording = { rec -> onPlayRecording(rec, folderRecordings) },
            onPlayAll = { shuffle -> onPlayAllInFolder(folderRecordings, shuffle) },
            onToggleLike = onToggleLike,
            onOpenRecordingOptions = onOpenRecordingOptions,
            modifier = modifier
        )
    } else {
        // Top-level Recordings screen: ONLY shows the 3 dedicated Recording Folders.
        // Individual recordings are NEVER shown outside of their folders.
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 14.dp,
                bottom = 140.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier
                .fillMaxSize()
                .testTag("recordings_folders_screen")
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ISOLATED VOICE VAULT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Recordings",
                            style = MaterialTheme.typography.displayLarge
                        )
                    }

                    FilledTonalButton(
                        onClick = onRequestDeviceScan,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("scan_recordings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Scan Device",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (uiState.isScanningDevice) "Scanning…" else "Scan Device")
                    }
                }
            }

            // Music Isolation Guarantee Banner
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(18.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Separated from Music Queue",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Call recordings, WhatsApp recordings, and voice memos stay strictly inside their folders below and never play between your music tracks.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "RECORDING FOLDERS (${RecordingCategory.entries.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(
                items = RecordingCategory.entries,
                key = { it.id }
            ) { category ->
                val folderItems = uiState.recordings.filter { it.recordingCategory == category.id }
                val totalDurationMs = folderItems.sumOf { it.durationMs }
                RecordingFolderCard(
                    category = category,
                    recordingCount = folderItems.size,
                    totalDurationMs = totalDurationMs,
                    onOpenFolder = { onSelectCategory(category) },
                    onImportToFolder = { onImportToCategory(category) },
                    modifier = Modifier.testTag("recording_folder_card_${category.id}")
                )
            }
        }
    }
}

@Composable
private fun RecordingFolderCard(
    category: RecordingCategory,
    recordingCount: Int,
    totalDurationMs: Long,
    onOpenFolder: () -> Unit,
    onImportToFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(category.colorHex)
    val folderIcon = categoryIcon(category)

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenFolder)
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.28f),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accentColor,
                                    accentColor.copy(alpha = 0.65f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = folderIcon,
                        contentDescription = category.folderTitle,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = accentColor.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = category.badgeLabel.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "$recordingCount ${if (recordingCount == 1) "file" else "files"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = category.folderTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = category.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open ${category.folderTitle}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (recordingCount > 0) {
                        "Total duration: ${formatDuration(totalDurationMs)} • Tap to view files inside folder"
                    } else {
                        "Folder is empty • Tap to open or import files"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = onImportToFolder,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("import_recording_folder_${category.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AudioFile,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Audio",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingFolderDetailView(
    category: RecordingCategory,
    folderRecordings: List<SongEntity>,
    sortOrder: FolderSortOrder,
    sortReversed: Boolean,
    currentSongId: Long?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onSortOrderChange: (FolderSortOrder) -> Unit,
    onImportIntoFolder: () -> Unit,
    onPlayRecording: (SongEntity) -> Unit,
    onPlayAll: (Boolean) -> Unit,
    onToggleLike: (SongEntity) -> Unit,
    onOpenRecordingOptions: (SongEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val accentColor = Color(category.colorHex)
    val folderIcon = categoryIcon(category)

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 140.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("recording_folder_detail_${category.id}")
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_from_recording_folder_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Recording Folders"
                        )
                    }
                    Text(
                        text = "Recording Folders",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onImportIntoFolder,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("import_to_recording_folder_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AudioFile,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import into Folder")
                }
            }
        }

        // Folder Hero Card
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.20f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = folderIcon,
                                contentDescription = category.folderTitle,
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category.badgeLabel.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = category.folderTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${folderRecordings.size} recordings • Isolated from Music Queue",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = category.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (folderRecordings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { onPlayAll(false) },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("play_recording_folder_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play Folder", color = Color.White)
                            }

                            FilledTonalButton(
                                onClick = { onPlayAll(true) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("shuffle_recording_folder_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shuffle,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shuffle Folder")
                            }
                        }
                    }
                }
            }
        }

        // Reversible Sort Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sort by:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FolderSortOrder.entries.forEach { order ->
                    val isSelected = sortOrder == order
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSortOrderChange(order) },
                        label = { Text(order.label) },
                        trailingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = if (sortReversed) {
                                        Icons.Filled.ArrowUpward
                                    } else {
                                        Icons.Filled.ArrowDownward
                                    },
                                    contentDescription = if (sortReversed) "Reversed order" else "Normal order",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.testTag("recording_sort_chip_${order.name.lowercase()}")
                    )
                }
            }
        }

        if (folderRecordings.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FolderSpecial,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No files in ${category.folderTitle}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Import audio files directly into this folder or move any track from your library using the 3-dot menu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(
                items = folderRecordings,
                key = { it.id }
            ) { recording ->
                SongRowItem(
                    song = recording,
                    isCurrentSong = recording.id == currentSongId,
                    isPlaying = recording.id == currentSongId && isPlaying,
                    onSongClick = { onPlayRecording(recording) },
                    onToggleLike = { onToggleLike(recording) },
                    onOpenOptions = { onOpenRecordingOptions(recording) }
                )
            }
        }
    }
}

private fun categoryIcon(category: RecordingCategory): ImageVector = when (category) {
    RecordingCategory.CALL_RECORDINGS -> Icons.Filled.PhoneInTalk
    RecordingCategory.WHATSAPP_RECORDINGS -> Icons.Filled.RecordVoiceOver
    RecordingCategory.VOICE_RECORDINGS -> Icons.Filled.Mic
}
