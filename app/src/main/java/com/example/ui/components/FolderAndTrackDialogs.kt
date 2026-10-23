package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AlbumFolderEntity
import com.example.data.RecordingCategory
import com.example.data.SingerFolderEntity
import com.example.data.SongEntity
import com.example.ui.theme.StudioCoverPalettes

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TrackOrganizationSheet(
    song: SongEntity,
    albums: List<AlbumFolderEntity>,
    singers: List<SingerFolderEntity>,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onToggleLike: () -> Unit,
    onAssignToAlbum: (AlbumFolderEntity) -> Unit,
    onAssignToSinger: (SingerFolderEntity) -> Unit,
    onMoveToRecordingFolder: (RecordingCategory?) -> Unit = {},
    onEditMetadata: () -> Unit,
    onDeleteSong: () -> Unit
) {
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
            // Song summary header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AlbumArtworkView(
                    colorHex = song.colorHex,
                    title = song.albumName,
                    artworkPath = song.artworkPath,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.singerName} • ${song.albumName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            // Quick Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionTileButton(
                    icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                    label = "Play Next",
                    onClick = {
                        onPlayNext()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sheet_play_next_button")
                )
                if (song.recordingCategory == null) {
                    ActionTileButton(
                        icon = if (song.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        label = if (song.isLiked) "Liked" else "Like Song",
                        onClick = {
                            onToggleLike()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_like_button")
                    )
                }
                ActionTileButton(
                    icon = Icons.Filled.Edit,
                    label = "Edit Info",
                    onClick = {
                        onDismiss()
                        onEditMetadata()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sheet_edit_metadata_button")
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Recording Folder / Music Library Isolation section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Audio Category & Recording Folders",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isMusicTrack = song.recordingCategory == null
                FilterChip(
                    selected = isMusicTrack,
                    onClick = {
                        onMoveToRecordingFolder(null)
                        onDismiss()
                    },
                    label = { Text("Music Library") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isMusicTrack) Icons.Filled.Check else Icons.Filled.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("move_to_music_library_chip")
                )

                RecordingCategory.entries.forEach { category ->
                    val isSelected = song.recordingCategory == category.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onMoveToRecordingFolder(category)
                            onDismiss()
                        },
                        label = { Text(category.folderTitle) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.testTag("move_to_recording_chip_${category.id}")
                    )
                }
            }

            if (song.recordingCategory == null) {
                Spacer(modifier = Modifier.height(18.dp))

                // Assign to Album Folder section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Album,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Move to Album Folder",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (albums.isEmpty()) {
                    Text(
                        text = "No Album folders yet. Create one in the Albums tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        albums.forEach { album ->
                            val isSelected = song.albumId == album.id || song.albumName.equals(album.title, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onAssignToAlbum(album)
                                    onDismiss()
                                },
                                label = { Text(album.title) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                modifier = Modifier.testTag("assign_album_chip_${album.id}")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Assign to Singer Folder section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Assign to Singer Folder",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (singers.isEmpty()) {
                    Text(
                        text = "No Singer folders yet. Create one in the Singers tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        singers.forEach { singer ->
                            val isSelected = song.singerId == singer.id || song.singerName.equals(singer.name, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onAssignToSinger(singer)
                                    onDismiss()
                                },
                                label = { Text(singer.name) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                modifier = Modifier.testTag("assign_singer_chip_${singer.id}")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    onDeleteSong()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sheet_delete_song_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (song.recordingCategory != null) "Delete Recording from Library" else "Remove Song from Library",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ActionTileButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun EditSongMetadataDialog(
    song: SongEntity,
    onDismiss: () -> Unit,
    onConfirm: (title: String, singer: String, album: String) -> Unit
) {
    var title by remember { mutableStateOf(song.title) }
    var singer by remember { mutableStateOf(song.singerName) }
    var album by remember { mutableStateOf(song.albumName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Song Metadata") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Song Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_song_title_input")
                )
                OutlinedTextField(
                    value = singer,
                    onValueChange = { singer = it },
                    label = { Text("Singer / Artist") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_song_singer_input")
                )
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Album Folder") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_song_album_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, singer, album)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_song_metadata_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AlbumFolderDialog(
    existingAlbum: AlbumFolderEntity? = null,
    allSongs: List<SongEntity>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        singerName: String,
        releaseYear: Int,
        description: String,
        coverColorHex: Long,
        selectedSongIds: List<Long>
    ) -> Unit
) {
    var title by remember { mutableStateOf(existingAlbum?.title ?: "") }
    var singerName by remember { mutableStateOf(existingAlbum?.singerName ?: "") }
    var yearText by remember { mutableStateOf(existingAlbum?.releaseYear?.toString() ?: "2026") }
    var description by remember { mutableStateOf(existingAlbum?.description ?: "") }
    var selectedColor by remember {
        mutableLongStateOf(existingAlbum?.coverColorHex ?: StudioCoverPalettes.first())
    }
    val selectedSongIds = remember {
        mutableStateListOf<Long>().apply {
            if (existingAlbum != null) {
                addAll(allSongs.filter { it.albumId == existingAlbum.id }.map { it.id })
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existingAlbum == null) "Create Album Folder" else "Edit Album Folder")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Album Title") },
                    placeholder = { Text("e.g., Late Night Sessions") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("album_title_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = singerName,
                        onValueChange = { singerName = it },
                        label = { Text("Primary Singer") },
                        placeholder = { Text("e.g., Klara Voss") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("album_singer_input")
                    )
                    OutlinedTextField(
                        value = yearText,
                        onValueChange = { yearText = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Year") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("album_year_input")
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Editorial Notes (Optional)") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("album_description_input")
                )

                Text(
                    text = "Cover Palette",
                    style = MaterialTheme.typography.labelLarge
                )
                ColorPaletteSelectorRow(
                    selectedColorHex = selectedColor,
                    onSelectColor = { selectedColor = it }
                )

                if (existingAlbum == null && allSongs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Include Songs in Album (${selectedSongIds.size} selected)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                    ) {
                        items(allSongs, key = { it.id }) { song ->
                            val checked = song.id in selectedSongIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (checked) selectedSongIds.remove(song.id)
                                        else selectedSongIds.add(song.id)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) selectedSongIds.add(song.id)
                                        else selectedSongIds.remove(song.id)
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.singerName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            title,
                            singerName.ifBlank { "Various Singers" },
                            yearText.toIntOrNull() ?: 2026,
                            description,
                            selectedColor,
                            selectedSongIds.toList()
                        )
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_save_album_button")
            ) {
                Text(if (existingAlbum == null) "Create Album" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SingerFolderDialog(
    existingSinger: SingerFolderEntity? = null,
    allSongs: List<SongEntity>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        genreTag: String,
        bio: String,
        avatarColorHex: Long,
        selectedSongIds: List<Long>
    ) -> Unit
) {
    var name by remember { mutableStateOf(existingSinger?.name ?: "") }
    var genreTag by remember { mutableStateOf(existingSinger?.genreTag ?: "Alternative & Electronic") }
    var bio by remember { mutableStateOf(existingSinger?.bio ?: "") }
    var selectedColor by remember {
        mutableLongStateOf(existingSinger?.avatarColorHex ?: StudioCoverPalettes[1])
    }
    val selectedSongIds = remember {
        mutableStateListOf<Long>().apply {
            if (existingSinger != null) {
                addAll(allSongs.filter { it.singerId == existingSinger.id }.map { it.id })
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existingSinger == null) "Create Singer Folder" else "Edit Singer Folder")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Singer / Artist Name") },
                    placeholder = { Text("e.g., Sora Takahashi") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("singer_name_input")
                )
                OutlinedTextField(
                    value = genreTag,
                    onValueChange = { genreTag = it },
                    label = { Text("Genre / Style") },
                    placeholder = { Text("e.g., Indie Acoustic") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("singer_genre_input")
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Artist Notes / Bio (Optional)") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("singer_bio_input")
                )

                Text(
                    text = "Studio Accent Color",
                    style = MaterialTheme.typography.labelLarge
                )
                ColorPaletteSelectorRow(
                    selectedColorHex = selectedColor,
                    onSelectColor = { selectedColor = it }
                )

                if (existingSinger == null && allSongs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Assign Songs to Singer (${selectedSongIds.size} selected)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                    ) {
                        items(allSongs, key = { it.id }) { song ->
                            val checked = song.id in selectedSongIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (checked) selectedSongIds.remove(song.id)
                                        else selectedSongIds.add(song.id)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) selectedSongIds.add(song.id)
                                        else selectedSongIds.remove(song.id)
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.albumName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name,
                            genreTag,
                            bio,
                            selectedColor,
                            selectedSongIds.toList()
                        )
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_save_singer_button")
            ) {
                Text(if (existingSinger == null) "Create Singer" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ColorPaletteSelectorRow(
    selectedColorHex: Long,
    onSelectColor: (Long) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StudioCoverPalettes.forEach { hex ->
            val isSelected = hex == selectedColorHex
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(hex))
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.25f),
                        shape = CircleShape
                    )
                    .clickable { onSelectColor(hex) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected color",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
