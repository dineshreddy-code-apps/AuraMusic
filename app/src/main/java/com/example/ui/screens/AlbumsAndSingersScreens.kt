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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AlbumFolderEntity
import com.example.data.SingerFolderEntity
import com.example.data.SongEntity
import com.example.ui.FolderSortOrder
import com.example.ui.MusicUiState
import com.example.ui.components.AlbumArtworkView
import com.example.ui.components.GoogleFilesSelectionIndicator
import com.example.ui.components.SingerAvatarView
import com.example.ui.components.SongRowItem
import com.example.ui.components.formatDuration
import com.example.ui.components.longPressMoreThanOneSecond

@Composable
fun AlbumsSectionScreen(
    uiState: MusicUiState,
    onSelectAlbum: (Long?) -> Unit,
    onSortOrderChange: (FolderSortOrder) -> Unit,
    onOpenCreateAlbum: () -> Unit,
    onEditAlbum: (AlbumFolderEntity) -> Unit,
    onDeleteAlbum: (AlbumFolderEntity) -> Unit,
    onAssignSongToAlbum: (SongEntity, AlbumFolderEntity) -> Unit,
    onPlaySong: (SongEntity, List<SongEntity>) -> Unit,
    onPlayAll: (List<SongEntity>, Boolean) -> Unit,
    onToggleLike: (SongEntity) -> Unit,
    onOpenSongOptions: (SongEntity) -> Unit,
    onToggleSelectAlbum: (Long) -> Unit = {},
    onToggleSelectSong: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeAlbum = uiState.albums.find { it.id == uiState.selectedAlbumId }
    if (activeAlbum != null) {
        val albumSongs = uiState.songs.filter {
            it.albumId == activeAlbum.id || it.albumName.equals(activeAlbum.title, ignoreCase = true)
        }
        AlbumDetailView(
            album = activeAlbum,
            albumSongs = albumSongs,
            allSongs = uiState.songs,
            currentSongId = uiState.playback.currentSong?.id,
            isPlaying = uiState.playback.isPlaying,
            selectedSongIds = uiState.selectedSongIds,
            isSelectionMode = uiState.isSelectionMode,
            onBack = { onSelectAlbum(null) },
            onEditAlbum = { onEditAlbum(activeAlbum) },
            onDeleteAlbum = { onDeleteAlbum(activeAlbum) },
            onAssignSongs = { songsToAssign ->
                songsToAssign.forEach { onAssignSongToAlbum(it, activeAlbum) }
            },
            onPlaySong = { song -> onPlaySong(song, albumSongs) },
            onPlayAll = { shuffle -> onPlayAll(albumSongs, shuffle) },
            onToggleLike = onToggleLike,
            onOpenSongOptions = onOpenSongOptions,
            onToggleSelectSong = onToggleSelectSong,
            modifier = modifier
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 14.dp,
                bottom = 140.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = modifier
                .fillMaxSize()
                .testTag("albums_grid_screen")
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ORGANIZED COLLECTIONS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Albums",
                                style = MaterialTheme.typography.displayLarge
                            )
                        }

                        Button(
                            onClick = onOpenCreateAlbum,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("create_album_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Album")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FolderSortOrder.entries.forEach { order ->
                            val isSelected = uiState.albumSortOrder == order
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSortOrderChange(order) },
                                label = { Text(order.label) },
                                trailingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = if (uiState.albumSortReversed) {
                                                Icons.Filled.ArrowUpward
                                            } else {
                                                Icons.Filled.ArrowDownward
                                            },
                                            contentDescription = if (uiState.albumSortReversed) "Reversed order" else "Standard order",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                modifier = Modifier.testTag("album_sort_chip_${order.name}")
                            )
                        }
                    }
                }
            }

            items(uiState.albums, key = { it.id }) { album ->
                val albumTracks = uiState.songs.filter {
                    it.albumId == album.id || it.albumName.equals(album.title, ignoreCase = true)
                }
                val count = albumTracks.size
                val totalPlays = album.playCount + albumTracks.sumOf { it.playCount }
                val resolvedArtwork = album.artworkPath
                    ?: albumTracks.firstOrNull { !it.artworkPath.isNullOrBlank() }?.artworkPath
                val isSelected = album.id in uiState.selectedAlbumIds
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .then(
                            if (isSelected) {
                                Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                    .border(
                                        width = 1.6.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(18.dp)
                                    )
                            } else {
                                Modifier
                            }
                        )
                        .longPressMoreThanOneSecond(
                            key = album.id to uiState.isSelectionMode,
                            onClick = {
                                if (uiState.isSelectionMode) {
                                    onToggleSelectAlbum(album.id)
                                } else {
                                    onSelectAlbum(album.id)
                                }
                            },
                            onLongPressMoreThanOneSecond = { onToggleSelectAlbum(album.id) }
                        )
                        .padding(6.dp)
                        .testTag("album_card_${album.id}")
                ) {
                    Box {
                        AlbumArtworkView(
                            colorHex = album.coverColorHex,
                            title = album.title,
                            cornerRadius = 18.dp,
                            artworkPath = resolvedArtwork,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .shadow(8.dp, RoundedCornerShape(18.dp))
                        )
                        if (uiState.isSelectionMode || isSelected) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                GoogleFilesSelectionIndicator(
                                    isSelected = isSelected,
                                    size = 24.dp,
                                    modifier = Modifier.padding(2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${album.singerName} • $count song${if (count == 1) "" else "s"} • $totalPlays plays",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumDetailView(
    album: AlbumFolderEntity,
    albumSongs: List<SongEntity>,
    allSongs: List<SongEntity>,
    currentSongId: Long?,
    isPlaying: Boolean,
    selectedSongIds: Set<Long> = emptySet(),
    isSelectionMode: Boolean = false,
    onBack: () -> Unit,
    onEditAlbum: () -> Unit,
    onDeleteAlbum: () -> Unit,
    onAssignSongs: (List<SongEntity>) -> Unit,
    onPlaySong: (SongEntity) -> Unit,
    onPlayAll: (Boolean) -> Unit,
    onToggleLike: (SongEntity) -> Unit,
    onOpenSongOptions: (SongEntity) -> Unit,
    onToggleSelectSong: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var showAddSongsDialog by remember { mutableStateOf(false) }
    val accentColor = Color(album.coverColorHex)
    val totalDurationMs = albumSongs.sumOf { it.durationMs }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("album_detail_screen"),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.28f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("album_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Albums"
                        )
                    }

                    Row {
                        IconButton(
                            onClick = { showAddSongsDialog = true },
                            modifier = Modifier.testTag("album_add_songs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LibraryAdd,
                                contentDescription = "Add songs to album"
                            )
                        }
                        IconButton(
                            onClick = onEditAlbum,
                            modifier = Modifier.testTag("album_edit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit album folder"
                            )
                        }
                        IconButton(
                            onClick = onDeleteAlbum,
                            modifier = Modifier.testTag("album_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete album folder",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val resolvedAlbumArtwork = album.artworkPath
                    ?: albumSongs.firstOrNull { !it.artworkPath.isNullOrBlank() }?.artworkPath
                AlbumArtworkView(
                    colorHex = album.coverColorHex,
                    title = album.title,
                    cornerRadius = 22.dp,
                    artworkPath = resolvedAlbumArtwork,
                    modifier = Modifier
                        .size(190.dp)
                        .shadow(16.dp, RoundedCornerShape(22.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = album.title,
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = album.singerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ALBUM • ${album.releaseYear} • ${albumSongs.size} SONGS (${formatDuration(totalDurationMs)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (album.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = album.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onPlayAll(false) },
                        enabled = albumSongs.isNotEmpty(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play", fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = { onPlayAll(true) },
                        enabled = albumSongs.isNotEmpty(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (albumSongs.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No songs in this Album folder yet",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { showAddSongsDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Songs from Library")
                    }
                }
            }
        } else {
            itemsIndexed(albumSongs, key = { _, s -> s.id }) { idx, song ->
                SongRowItem(
                    song = song,
                    isCurrentSong = currentSongId == song.id,
                    isPlaying = isPlaying,
                    onSongClick = { onPlaySong(song) },
                    onToggleLike = { onToggleLike(song) },
                    onOpenOptions = { onOpenSongOptions(song) },
                    trackIndexNumber = idx + 1,
                    isSelected = song.id in selectedSongIds,
                    isSelectionMode = isSelectionMode,
                    onLongPressSelect = { onToggleSelectSong(song.id) },
                    onToggleSelect = { onToggleSelectSong(song.id) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }
        }
    }

    if (showAddSongsDialog) {
        MultiSelectSongsPickerDialog(
            title = "Add Songs to ${album.title}",
            allSongs = allSongs,
            alreadyAssignedIds = albumSongs.map { it.id }.toSet(),
            onDismiss = { showAddSongsDialog = false },
            onConfirm = onAssignSongs
        )
    }
}

@Composable
fun SingersSectionScreen(
    uiState: MusicUiState,
    onSelectSinger: (Long?) -> Unit,
    onSortOrderChange: (FolderSortOrder) -> Unit,
    onOpenCreateSinger: () -> Unit,
    onEditSinger: (SingerFolderEntity) -> Unit,
    onDeleteSinger: (SingerFolderEntity) -> Unit,
    onAssignSongToSinger: (SongEntity, SingerFolderEntity) -> Unit,
    onPlaySong: (SongEntity, List<SongEntity>) -> Unit,
    onPlayAll: (List<SongEntity>, Boolean) -> Unit,
    onToggleLike: (SongEntity) -> Unit,
    onOpenSongOptions: (SongEntity) -> Unit,
    onToggleSelectSinger: (Long) -> Unit = {},
    onToggleSelectSong: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeSinger = uiState.singers.find { it.id == uiState.selectedSingerId }
    if (activeSinger != null) {
        val singerSongs = uiState.songs.filter {
            it.singerId == activeSinger.id || it.singerName.equals(activeSinger.name, ignoreCase = true)
        }
        SingerDetailView(
            singer = activeSinger,
            singerSongs = singerSongs,
            allSongs = uiState.songs,
            currentSongId = uiState.playback.currentSong?.id,
            isPlaying = uiState.playback.isPlaying,
            selectedSongIds = uiState.selectedSongIds,
            isSelectionMode = uiState.isSelectionMode,
            onBack = { onSelectSinger(null) },
            onEditSinger = { onEditSinger(activeSinger) },
            onDeleteSinger = { onDeleteSinger(activeSinger) },
            onAssignSongs = { selected ->
                selected.forEach { onAssignSongToSinger(it, activeSinger) }
            },
            onPlaySong = { song -> onPlaySong(song, singerSongs) },
            onPlayAll = { shuffle -> onPlayAll(singerSongs, shuffle) },
            onToggleLike = onToggleLike,
            onOpenSongOptions = onOpenSongOptions,
            onToggleSelectSong = onToggleSelectSong,
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("singers_list_screen"),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ARTIST DIRECTORIES",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Singers",
                                style = MaterialTheme.typography.displayLarge
                            )
                        }

                        Button(
                            onClick = onOpenCreateSinger,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("create_singer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Singer")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FolderSortOrder.entries.forEach { order ->
                            val isSelected = uiState.singerSortOrder == order
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSortOrderChange(order) },
                                label = { Text(order.label) },
                                trailingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = if (uiState.singerSortReversed) {
                                                Icons.Filled.ArrowUpward
                                            } else {
                                                Icons.Filled.ArrowDownward
                                            },
                                            contentDescription = if (uiState.singerSortReversed) "Reversed order" else "Standard order",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                modifier = Modifier.testTag("singer_sort_chip_${order.name}")
                            )
                        }
                    }
                }
            }

            items(uiState.singers, key = { it.id }) { singer ->
                val singerSongs = uiState.songs.filter {
                    it.singerId == singer.id || it.singerName.equals(singer.name, ignoreCase = true)
                }
                val albumCount = singerSongs.map { it.albumName.lowercase() }.distinct().size
                val totalPlays = singer.playCount + singerSongs.sumOf { it.playCount }
                val isSelected = singer.id in uiState.selectedSingerIds

                Surface(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            width = if (isSelected) 1.6.dp else 0.8.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            },
                            shape = RoundedCornerShape(18.dp)
                        )
                        .longPressMoreThanOneSecond(
                            key = singer.id to uiState.isSelectionMode,
                            onClick = {
                                if (uiState.isSelectionMode) {
                                    onToggleSelectSinger(singer.id)
                                } else {
                                    onSelectSinger(singer.id)
                                }
                            },
                            onLongPressMoreThanOneSecond = { onToggleSelectSinger(singer.id) }
                        )
                        .testTag("singer_card_${singer.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.isSelectionMode || isSelected) {
                            GoogleFilesSelectionIndicator(
                                isSelected = isSelected,
                                size = 24.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        val resolvedSingerArt = singer.artworkPath
                            ?: singerSongs.firstOrNull { !it.artworkPath.isNullOrBlank() }?.artworkPath
                        SingerAvatarView(
                            colorHex = singer.avatarColorHex,
                            singerName = singer.name,
                            artworkPath = resolvedSingerArt,
                            modifier = Modifier.size(58.dp)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = singer.name,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = singer.genreTag,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${singerSongs.size} song${if (singerSongs.size == 1) "" else "s"} • $albumCount album${if (albumCount == 1) "" else "s"} • $totalPlays plays",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                if (singerSongs.isNotEmpty()) {
                                    onPlayAll(singerSongs, false)
                                } else {
                                    onSelectSinger(singer.id)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play singer tracks",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SingerDetailView(
    singer: SingerFolderEntity,
    singerSongs: List<SongEntity>,
    allSongs: List<SongEntity>,
    currentSongId: Long?,
    isPlaying: Boolean,
    selectedSongIds: Set<Long> = emptySet(),
    isSelectionMode: Boolean = false,
    onBack: () -> Unit,
    onEditSinger: () -> Unit,
    onDeleteSinger: () -> Unit,
    onAssignSongs: (List<SongEntity>) -> Unit,
    onPlaySong: (SongEntity) -> Unit,
    onPlayAll: (Boolean) -> Unit,
    onToggleLike: (SongEntity) -> Unit,
    onOpenSongOptions: (SongEntity) -> Unit,
    onToggleSelectSong: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var showAssignDialog by remember { mutableStateOf(false) }
    val accentColor = Color(singer.avatarColorHex)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("singer_detail_screen"),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.28f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("singer_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Singers"
                        )
                    }

                    Row {
                        IconButton(
                            onClick = { showAssignDialog = true },
                            modifier = Modifier.testTag("singer_add_songs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LibraryAdd,
                                contentDescription = "Assign songs to singer"
                            )
                        }
                        IconButton(
                            onClick = onEditSinger,
                            modifier = Modifier.testTag("singer_edit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit singer folder"
                            )
                        }
                        IconButton(
                            onClick = onDeleteSinger,
                            modifier = Modifier.testTag("singer_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete singer folder",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val resolvedSingerDetailArt = singer.artworkPath
                    ?: singerSongs.firstOrNull { !it.artworkPath.isNullOrBlank() }?.artworkPath
                SingerAvatarView(
                    colorHex = singer.avatarColorHex,
                    singerName = singer.name,
                    artworkPath = resolvedSingerDetailArt,
                    modifier = Modifier
                        .size(136.dp)
                        .shadow(16.dp, CircleShape)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = singer.name,
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${singer.genreTag} • ${singerSongs.size} Songs",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                if (singer.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = singer.bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onPlayAll(false) },
                        enabled = singerSongs.isNotEmpty(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play All", fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = { onPlayAll(true) },
                        enabled = singerSongs.isNotEmpty(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (singerSongs.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No songs assigned to ${singer.name} yet",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { showAssignDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Assign Songs from Library")
                    }
                }
            }
        } else {
            items(singerSongs, key = { it.id }) { song ->
                SongRowItem(
                    song = song,
                    isCurrentSong = currentSongId == song.id,
                    isPlaying = isPlaying,
                    onSongClick = { onPlaySong(song) },
                    onToggleLike = { onToggleLike(song) },
                    onOpenOptions = { onOpenSongOptions(song) },
                    isSelected = song.id in selectedSongIds,
                    isSelectionMode = isSelectionMode,
                    onLongPressSelect = { onToggleSelectSong(song.id) },
                    onToggleSelect = { onToggleSelectSong(song.id) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }
        }
    }

    if (showAssignDialog) {
        MultiSelectSongsPickerDialog(
            title = "Assign Songs to ${singer.name}",
            allSongs = allSongs,
            alreadyAssignedIds = singerSongs.map { it.id }.toSet(),
            onDismiss = { showAssignDialog = false },
            onConfirm = onAssignSongs
        )
    }
}

@Composable
fun LikedSongsScreen(
    uiState: MusicUiState,
    onPlaySong: (SongEntity) -> Unit,
    onPlayAll: (Boolean) -> Unit,
    onToggleLike: (SongEntity) -> Unit,
    onOpenSongOptions: (SongEntity) -> Unit,
    onToggleSelectSong: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("liked_songs_screen"),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "FAVORITE COLLECTION",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Liked Songs",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Text(
                            text = "${uiState.likedSongs.size} favorited track${if (uiState.likedSongs.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (uiState.likedSongs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onPlayAll(false) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("play_liked_songs_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play Liked", fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = { onPlayAll(true) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("shuffle_liked_songs_button")
                        ) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (uiState.likedSongs.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Liked Songs Yet",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap the heart icon on any song or inside the Now Playing studio to save your favorites here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(uiState.likedSongs, key = { it.id }) { song ->
                SongRowItem(
                    song = song,
                    isCurrentSong = uiState.playback.currentSong?.id == song.id,
                    isPlaying = uiState.playback.isPlaying,
                    onSongClick = { onPlaySong(song) },
                    onToggleLike = { onToggleLike(song) },
                    onOpenOptions = { onOpenSongOptions(song) },
                    isSelected = song.id in uiState.selectedSongIds,
                    isSelectionMode = uiState.isSelectionMode,
                    onLongPressSelect = { onToggleSelectSong(song.id) },
                    onToggleSelect = { onToggleSelectSong(song.id) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun MultiSelectSongsPickerDialog(
    title: String,
    allSongs: List<SongEntity>,
    alreadyAssignedIds: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (List<SongEntity>) -> Unit
) {
    val newlySelected = remember { mutableStateListOf<SongEntity>() }
    val availableSongs = allSongs.filter { it.id !in alreadyAssignedIds }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (availableSongs.isEmpty()) {
                Text(
                    text = "All songs in your library are already included in this folder.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                ) {
                    items(availableSongs, key = { it.id }) { song ->
                        val isChecked = song in newlySelected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) newlySelected.remove(song)
                                    else newlySelected.add(song)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) newlySelected.add(song)
                                    else newlySelected.remove(song)
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${song.singerName} • ${song.albumName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(newlySelected.toList())
                    onDismiss()
                },
                enabled = newlySelected.isNotEmpty()
            ) {
                Text("Add (${newlySelected.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
