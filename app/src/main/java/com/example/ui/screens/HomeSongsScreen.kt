package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.AlbumFolderEntity
import com.example.data.SingerFolderEntity
import com.example.data.SongEntity
import com.example.ui.MusicUiState
import com.example.ui.PrimaryTab
import com.example.ui.SongSortOrder
import com.example.ui.components.AlbumArtworkView
import com.example.ui.components.AuraBrandLogo
import com.example.ui.components.SingerAvatarView
import com.example.ui.components.SongRowItem
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.AuraCrimsonDark
import com.example.ui.theme.AuraEmerald
import com.example.ui.theme.AuraIndigo

import androidx.compose.ui.platform.LocalFocusManager
import com.example.ui.components.GoogleFilesSelectionIndicator
import com.example.ui.components.longPressMoreThanOneSecond

@Composable
fun HomeSongsScreen(
    uiState: MusicUiState,
    onRequestDeviceScan: () -> Unit,
    onRequestImportFiles: () -> Unit,
    onRequestImportZip: () -> Unit,
    onRestoreStudioSamples: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortOrderChange: (SongSortOrder) -> Unit,
    onCycleThemeMode: () -> Unit,
    onNavigateTab: (PrimaryTab) -> Unit,
    onOpenAlbumFromSearch: (Long) -> Unit,
    onOpenSingerFromSearch: (Long) -> Unit,
    onPlaySong: (SongEntity) -> Unit,
    onPlayAll: (Boolean) -> Unit,
    onToggleLike: (SongEntity) -> Unit,
    onOpenSongOptions: (SongEntity) -> Unit,
    onToggleSelectSong: (Long) -> Unit = {},
    onToggleSelectAlbum: (Long) -> Unit = {},
    onToggleSelectSinger: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isSearching = uiState.searchQuery.isNotBlank()
    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_songs_screen"),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // Editorial Top Header + Brand Logo + Theme Switcher
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AuraBrandLogo(size = 44.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AURA OFFLINE STUDIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Library",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onCycleThemeMode)
                        .testTag("theme_toggle_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (uiState.themeMode) {
                                AppThemeMode.LIGHT -> Icons.Filled.LightMode
                                AppThemeMode.DARK -> Icons.Filled.DarkMode
                                AppThemeMode.SYSTEM -> Icons.Filled.GraphicEq
                            },
                            contentDescription = "Switch appearance theme",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = uiState.themeMode.label,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Offline Storage Ingestion Banner (Scan Device + Import Audio Files + Import ZIP Archive)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    AuraIndigo.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Offline Device Music",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${uiState.songs.size} tracks • ${uiState.albums.size} albums • ${uiState.singers.size} singers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (uiState.isScanningDevice) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onRequestDeviceScan,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scan_device_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Scan", style = MaterialTheme.typography.labelLarge)
                        }

                        FilledTonalButton(
                            onClick = onRequestImportFiles,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_audio_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Audio", style = MaterialTheme.typography.labelLarge)
                        }

                        FilledTonalButton(
                            onClick = onRequestImportZip,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_zip_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Album,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("ZIP File", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // Quick Collection Bento Tiles (Albums, Singers, Liked Songs)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickFolderCard(
                    title = "Albums",
                    subtitle = "${uiState.albums.size} folders",
                    icon = Icons.Filled.Album,
                    accentColor = AuraIndigo,
                    onClick = { onNavigateTab(PrimaryTab.ALBUMS) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_card_albums")
                )
                QuickFolderCard(
                    title = "Singers",
                    subtitle = "${uiState.singers.size} artists",
                    icon = Icons.Filled.Person,
                    accentColor = AuraEmerald,
                    onClick = { onNavigateTab(PrimaryTab.SINGERS) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_card_singers")
                )
                QuickFolderCard(
                    title = "Liked",
                    subtitle = "${uiState.likedSongs.size} songs",
                    icon = Icons.Filled.Favorite,
                    accentColor = AuraCrimsonDark,
                    onClick = { onNavigateTab(PrimaryTab.LIKED) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_card_liked")
                )
            }
        }

        // Global Search Bar placed directly below Albums, Singers, and Liked buttons on Home page
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 14.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search songs, albums, or singers by name…") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Global search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.testTag("clear_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_songs_input")
                )

                if (isSearching) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Found ${uiState.filteredSongs.size} song${if (uiState.filteredSongs.size == 1) "" else "s"} • ${uiState.filteredAlbums.size} album${if (uiState.filteredAlbums.size == 1) "" else "s"} • ${uiState.filteredSingers.size} singer${if (uiState.filteredSingers.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // When actively searching, show Matching Albums and Matching Singers right below Global Search Bar
        if (isSearching && uiState.filteredAlbums.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Matching Albums (${uiState.filteredAlbums.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.testTag("search_results_albums_row")
                    ) {
                        items(uiState.filteredAlbums, key = { it.id }) { album ->
                            val albumTracks = uiState.songs.filter {
                                it.albumId == album.id || it.albumName.equals(album.title, ignoreCase = true)
                            }
                            val resolvedArtwork = album.artworkPath
                                ?: albumTracks.firstOrNull { !it.artworkPath.isNullOrBlank() }?.artworkPath
                            SearchAlbumResultCard(
                                album = album,
                                songCount = albumTracks.size,
                                artworkPath = resolvedArtwork,
                                isSelected = album.id in uiState.selectedAlbumIds,
                                isSelectionMode = uiState.isSelectionMode,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        onToggleSelectAlbum(album.id)
                                    } else {
                                        focusManager.clearFocus()
                                        onOpenAlbumFromSearch(album.id)
                                    }
                                },
                                onLongPressSelect = { onToggleSelectAlbum(album.id) }
                            )
                        }
                    }
                }
            }
        }

        if (isSearching && uiState.filteredSingers.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Matching Singers (${uiState.filteredSingers.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.testTag("search_results_singers_row")
                    ) {
                        items(uiState.filteredSingers, key = { it.id }) { singer ->
                            val singerTracks = uiState.songs.filter {
                                it.singerId == singer.id || it.singerName.equals(singer.name, ignoreCase = true)
                            }
                            val resolvedArt = singer.artworkPath
                                ?: singerTracks.firstOrNull { !it.artworkPath.isNullOrBlank() }?.artworkPath
                            SearchSingerResultCard(
                                singer = singer,
                                songCount = singerTracks.size,
                                artworkPath = resolvedArt,
                                isSelected = singer.id in uiState.selectedSingerIds,
                                isSelectionMode = uiState.isSelectionMode,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        onToggleSelectSinger(singer.id)
                                    } else {
                                        focusManager.clearFocus()
                                        onOpenSingerFromSearch(singer.id)
                                    }
                                },
                                onLongPressSelect = { onToggleSelectSinger(singer.id) }
                            )
                        }
                    }
                }
            }
        }

        // Songs Section Header + Sort Chips
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                if (isSearching && uiState.filteredSongs.isNotEmpty()) {
                    Text(
                        text = "Matching Songs (${uiState.filteredSongs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SongSortOrder.entries.forEach { order ->
                        val isSelected = uiState.sortOrder == order
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSortOrderChange(order) },
                            label = { Text(order.label) },
                            trailingIcon = if (isSelected) {
                                {
                                    Text(
                                        text = if (uiState.songSortReversed) "↑" else "↓",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else null,
                            modifier = Modifier.testTag("sort_chip_${order.name}")
                        )
                    }
                }
            }
        }

        // Play All & Shuffle Action Bar
        if (uiState.filteredSongs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (isSearching) focusManager.clearFocus()
                            onPlayAll(false)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("play_all_songs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play", fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = {
                            if (isSearching) focusManager.clearFocus()
                            onPlayAll(true)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("shuffle_all_songs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Shuffle",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Song Rows or Empty State
        val noSearchResultsAtAll = isSearching &&
            uiState.filteredSongs.isEmpty() &&
            uiState.filteredAlbums.isEmpty() &&
            uiState.filteredSingers.isEmpty()

        if (noSearchResultsAtAll || (!isSearching && uiState.filteredSongs.isEmpty())) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSearching) {
                            "No songs, albums, or singers found"
                        } else {
                            "Your offline library is empty"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isSearching) {
                            "Try searching for a different track title, album folder, or singer name."
                        } else {
                            "Scan your device storage, import audio files, or load studio acoustic tracks."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!isSearching) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onRestoreStudioSamples,
                            modifier = Modifier.testTag("restore_samples_button")
                        ) {
                            Text("Load Studio Acoustic Tracks")
                        }
                    }
                }
            }
        } else {
            items(
                items = uiState.filteredSongs,
                key = { it.id }
            ) { song ->
                val isCurrent = uiState.playback.currentSong?.id == song.id
                val isSelected = song.id in uiState.selectedSongIds
                SongRowItem(
                    song = song,
                    isCurrentSong = isCurrent,
                    isPlaying = uiState.playback.isPlaying,
                    onSongClick = {
                        if (isSearching) focusManager.clearFocus()
                        onPlaySong(song)
                    },
                    onToggleLike = { onToggleLike(song) },
                    onOpenOptions = { onOpenSongOptions(song) },
                    isSelected = isSelected,
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
private fun SearchAlbumResultCard(
    album: AlbumFolderEntity,
    songCount: Int,
    artworkPath: String?,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongPressSelect: () -> Unit = {}
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = Modifier
            .width(148.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 1.6.dp else 0.8.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
            .longPressMoreThanOneSecond(
                key = album.id to isSelectionMode,
                onClick = onClick,
                onLongPressMoreThanOneSecond = onLongPressSelect
            )
            .testTag("search_album_result_${album.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box {
                AlbumArtworkView(
                    colorHex = album.coverColorHex,
                    title = album.title,
                    cornerRadius = 12.dp,
                    artworkPath = artworkPath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(116.dp)
                )
                if (isSelectionMode || isSelected) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        GoogleFilesSelectionIndicator(
                            isSelected = isSelected,
                            size = 24.dp,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = album.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${album.singerName} • $songCount song${if (songCount == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchSingerResultCard(
    singer: SingerFolderEntity,
    songCount: Int,
    artworkPath: String?,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongPressSelect: () -> Unit = {}
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = Modifier
            .width(196.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 1.6.dp else 0.8.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
            .longPressMoreThanOneSecond(
                key = singer.id to isSelectionMode,
                onClick = onClick,
                onLongPressMoreThanOneSecond = onLongPressSelect
            )
            .testTag("search_singer_result_${singer.id}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode || isSelected) {
                GoogleFilesSelectionIndicator(
                    isSelected = isSelected,
                    size = 22.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            SingerAvatarView(
                colorHex = singer.avatarColorHex,
                singerName = singer.name,
                artworkPath = artworkPath,
                modifier = Modifier.size(46.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = singer.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$songCount song${if (songCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun QuickFolderCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 0.8.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
