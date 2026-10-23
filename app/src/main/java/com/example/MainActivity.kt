package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AlbumFolderEntity
import com.example.data.RecordingCategory
import com.example.data.SingerFolderEntity
import com.example.data.SongEntity
import com.example.ui.MusicUiState
import com.example.ui.MusicViewModel
import com.example.ui.PrimaryTab
import com.example.ui.components.AlbumFolderDialog
import com.example.ui.components.AudioShareHelper
import com.example.ui.components.EditSongMetadataDialog
import com.example.ui.components.FullScreenNowPlayingView
import com.example.ui.components.MultiSelectionShareBar
import com.example.ui.components.PersistentMiniPlayer
import com.example.ui.components.SingerFolderDialog
import com.example.ui.components.TrackOrganizationSheet
import com.example.ui.screens.AlbumsSectionScreen
import com.example.ui.screens.HomeSongsScreen
import com.example.ui.screens.LikedSongsScreen
import com.example.ui.screens.RecordingsSectionScreen
import com.example.ui.screens.SingersSectionScreen
import com.example.ui.theme.AuraMusicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val musicViewModel: MusicViewModel = viewModel()
            val uiState by musicViewModel.uiState.collectAsStateWithLifecycle()

            AuraMusicTheme(themeMode = uiState.themeMode) {
                AuraMusicApp(
                    uiState = uiState,
                    viewModel = musicViewModel
                )
            }
        }
    }
}

@Composable
fun AuraMusicApp(
    uiState: MusicUiState,
    viewModel: MusicViewModel
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog & BottomSheet UI State
    var songForOptionsSheet by remember { mutableStateOf<SongEntity?>(null) }
    var songForMetadataEdit by remember { mutableStateOf<SongEntity?>(null) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var albumToEdit by remember { mutableStateOf<AlbumFolderEntity?>(null) }
    var showCreateSingerDialog by remember { mutableStateOf(false) }
    var singerToEdit by remember { mutableStateOf<SingerFolderEntity?>(null) }
    var targetRecordingCategoryForImport by remember { mutableStateOf<RecordingCategory?>(null) }

    // Permission launcher for scanning local device audio
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Media notification will display when granted */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!notifGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.scanDeviceStorage()
        } else {
            // Even if denied, perform a safe MediaStore check or notify user
            viewModel.scanDeviceStorage()
        }
    }

    // Storage Access Framework multi-audio picker launcher (also handles .zip if selected)
    val importFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importAudioFiles(uris)
        }
    }

    // Storage Access Framework picker for importing audio directly into a specific Recording folder
    val importToRecordingFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val category = targetRecordingCategoryForImport
        if (uris.isNotEmpty() && category != null) {
            viewModel.importRecordingsToCategory(uris, category)
        }
    }

    // Dedicated Storage Access Framework ZIP archive picker launcher
    val importZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importZipArchives(uris)
        }
    }

    val requestDeviceScanAction = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            audioPermission
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.scanDeviceStorage()
        } else {
            permissionLauncher.launch(audioPermission)
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        val message = uiState.statusMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    PersistentMiniPlayer(
                        playback = uiState.playback,
                        onExpandNowPlaying = { viewModel.setNowPlayingExpanded(true) },
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onSkipPrevious = viewModel::skipPrevious,
                        onSkipNext = viewModel::skipNext,
                        onToggleLike = viewModel::toggleLike
                    )

                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        NavigationBarItem(
                            selected = uiState.selectedTab == PrimaryTab.SONGS,
                            onClick = { viewModel.selectTab(PrimaryTab.SONGS) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.selectedTab == PrimaryTab.SONGS) {
                                        Icons.Filled.MusicNote
                                    } else {
                                        Icons.Outlined.MusicNote
                                    },
                                    contentDescription = "Songs"
                                )
                            },
                            label = { Text("Songs") },
                            modifier = Modifier.testTag("nav_tab_songs")
                        )

                        NavigationBarItem(
                            selected = uiState.selectedTab == PrimaryTab.ALBUMS,
                            onClick = { viewModel.selectTab(PrimaryTab.ALBUMS) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.selectedTab == PrimaryTab.ALBUMS) {
                                        Icons.Filled.Album
                                    } else {
                                        Icons.Outlined.Album
                                    },
                                    contentDescription = "Albums"
                                )
                            },
                            label = { Text("Albums") },
                            modifier = Modifier.testTag("nav_tab_albums")
                        )

                        NavigationBarItem(
                            selected = uiState.selectedTab == PrimaryTab.SINGERS,
                            onClick = { viewModel.selectTab(PrimaryTab.SINGERS) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.selectedTab == PrimaryTab.SINGERS) {
                                        Icons.Filled.Person
                                    } else {
                                        Icons.Outlined.Person
                                    },
                                    contentDescription = "Singers"
                                )
                            },
                            label = { Text("Singers") },
                            modifier = Modifier.testTag("nav_tab_singers")
                        )

                        NavigationBarItem(
                            selected = uiState.selectedTab == PrimaryTab.LIKED,
                            onClick = { viewModel.selectTab(PrimaryTab.LIKED) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.selectedTab == PrimaryTab.LIKED) {
                                        Icons.Filled.Favorite
                                    } else {
                                        Icons.Outlined.FavoriteBorder
                                    },
                                    contentDescription = "Liked"
                                )
                            },
                            label = { Text("Liked") },
                            modifier = Modifier.testTag("nav_tab_liked")
                        )

                        NavigationBarItem(
                            selected = uiState.selectedTab == PrimaryTab.RECORDINGS,
                            onClick = { viewModel.selectTab(PrimaryTab.RECORDINGS) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.selectedTab == PrimaryTab.RECORDINGS) {
                                        Icons.Filled.Mic
                                    } else {
                                        Icons.Outlined.Mic
                                    },
                                    contentDescription = "Recordings"
                                )
                            },
                            label = { Text("Recordings") },
                            modifier = Modifier.testTag("nav_tab_recordings")
                        )
                    }
                }
            }
        ) { innerPadding ->
            val resolvedSelectedTracks = remember(
                uiState.songs,
                uiState.recordings,
                uiState.albums,
                uiState.singers,
                uiState.selectedSongIds,
                uiState.selectedAlbumIds,
                uiState.selectedSingerIds
            ) {
                AudioShareHelper.resolveSongsForSelection(
                    allSongs = uiState.songs,
                    allRecordings = uiState.recordings,
                    allAlbums = uiState.albums,
                    allSingers = uiState.singers,
                    selectedSongIds = uiState.selectedSongIds,
                    selectedAlbumIds = uiState.selectedAlbumIds,
                    selectedSingerIds = uiState.selectedSingerIds
                )
            }

            val isAllSelectedInView = remember(
                uiState.selectedTab,
                uiState.selectedAlbumId,
                uiState.selectedSingerId,
                uiState.filteredSongs,
                uiState.albums,
                uiState.singers,
                uiState.likedSongs,
                uiState.songs,
                uiState.selectedSongIds,
                uiState.selectedAlbumIds,
                uiState.selectedSingerIds
            ) {
                when (uiState.selectedTab) {
                    PrimaryTab.SONGS -> uiState.filteredSongs.isNotEmpty() &&
                        uiState.selectedSongIds.containsAll(uiState.filteredSongs.map { it.id })
                    PrimaryTab.ALBUMS -> {
                        val activeAlbumId = uiState.selectedAlbumId
                        if (activeAlbumId != null) {
                            val activeAlbum = uiState.albums.find { it.id == activeAlbumId }
                            val albumSongs = if (activeAlbum != null) {
                                uiState.songs.filter {
                                    it.albumId == activeAlbum.id || it.albumName.equals(activeAlbum.title, ignoreCase = true)
                                }
                            } else emptyList()
                            albumSongs.isNotEmpty() && uiState.selectedSongIds.containsAll(albumSongs.map { it.id })
                        } else {
                            uiState.albums.isNotEmpty() && uiState.selectedAlbumIds.containsAll(uiState.albums.map { it.id })
                        }
                    }
                    PrimaryTab.SINGERS -> {
                        val activeSingerId = uiState.selectedSingerId
                        if (activeSingerId != null) {
                            val activeSinger = uiState.singers.find { it.id == activeSingerId }
                            val singerSongs = if (activeSinger != null) {
                                uiState.songs.filter {
                                    it.singerId == activeSinger.id || it.singerName.equals(activeSinger.name, ignoreCase = true)
                                }
                            } else emptyList()
                            singerSongs.isNotEmpty() && uiState.selectedSongIds.containsAll(singerSongs.map { it.id })
                        } else {
                            uiState.singers.isNotEmpty() && uiState.selectedSingerIds.containsAll(uiState.singers.map { it.id })
                        }
                    }
                    PrimaryTab.LIKED -> uiState.likedSongs.isNotEmpty() &&
                        uiState.selectedSongIds.containsAll(uiState.likedSongs.map { it.id })
                    PrimaryTab.RECORDINGS -> false
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                MultiSelectionShareBar(
                    selectedSongCount = uiState.selectedSongIds.size,
                    selectedAlbumCount = uiState.selectedAlbumIds.size,
                    selectedSingerCount = uiState.selectedSingerIds.size,
                    resolvedTracks = resolvedSelectedTracks,
                    isAllSelectedInView = isAllSelectedInView,
                    onToggleSelectAllCurrentView = viewModel::selectAllInCurrentView,
                    onShareSelected = {
                        val selectedAlbums = uiState.albums.filter { it.id in uiState.selectedAlbumIds }
                        val selectedSingers = uiState.singers.filter { it.id in uiState.selectedSingerIds }
                        AudioShareHelper.shareSelection(
                            context = context,
                            selectedSongs = resolvedSelectedTracks,
                            selectedAlbums = selectedAlbums,
                            selectedSingers = selectedSingers
                        )
                        viewModel.clearMultiSelection()
                    },
                    onClearSelection = viewModel::clearMultiSelection
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    when (uiState.selectedTab) {
                        PrimaryTab.SONGS -> {
                            HomeSongsScreen(
                                uiState = uiState,
                                onRequestDeviceScan = requestDeviceScanAction,
                                onRequestImportFiles = {
                                    importFilesLauncher.launch(
                                        arrayOf(
                                            "audio/*",
                                            "application/zip",
                                            "application/x-zip-compressed"
                                        )
                                    )
                                },
                                onRequestImportZip = {
                                    importZipLauncher.launch(
                                        arrayOf(
                                            "application/zip",
                                            "application/x-zip-compressed",
                                            "application/octet-stream"
                                        )
                                    )
                                },
                                onRestoreStudioSamples = viewModel::restoreStudioSamples,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onSortOrderChange = viewModel::updateSortOrder,
                                onCycleThemeMode = viewModel::cycleThemeMode,
                                onNavigateTab = viewModel::selectTab,
                                onOpenAlbumFromSearch = viewModel::navigateToAlbumFromSearch,
                                onOpenSingerFromSearch = viewModel::navigateToSingerFromSearch,
                                onPlaySong = { song -> viewModel.playSong(song, uiState.filteredSongs) },
                                onPlayAll = { shuffle -> viewModel.playAll(uiState.filteredSongs, shuffle) },
                                onToggleLike = viewModel::toggleLike,
                                onOpenSongOptions = { song -> songForOptionsSheet = song },
                                onToggleSelectSong = viewModel::toggleSelectSong,
                                onToggleSelectAlbum = viewModel::toggleSelectAlbum,
                                onToggleSelectSinger = viewModel::toggleSelectSinger
                            )
                        }

                        PrimaryTab.ALBUMS -> {
                            AlbumsSectionScreen(
                                uiState = uiState,
                                onSelectAlbum = viewModel::openAlbumDetail,
                                onSortOrderChange = viewModel::updateAlbumSortOrder,
                                onOpenCreateAlbum = { showCreateAlbumDialog = true },
                                onEditAlbum = { album -> albumToEdit = album },
                                onDeleteAlbum = viewModel::deleteAlbumFolder,
                                onAssignSongToAlbum = viewModel::assignSongToAlbum,
                                onPlaySong = viewModel::playSong,
                                onPlayAll = viewModel::playAll,
                                onToggleLike = viewModel::toggleLike,
                                onOpenSongOptions = { song -> songForOptionsSheet = song },
                                onToggleSelectAlbum = viewModel::toggleSelectAlbum,
                                onToggleSelectSong = viewModel::toggleSelectSong
                            )
                        }

                        PrimaryTab.SINGERS -> {
                            SingersSectionScreen(
                                uiState = uiState,
                                onSelectSinger = viewModel::openSingerDetail,
                                onSortOrderChange = viewModel::updateSingerSortOrder,
                                onOpenCreateSinger = { showCreateSingerDialog = true },
                                onEditSinger = { singer -> singerToEdit = singer },
                                onDeleteSinger = viewModel::deleteSingerFolder,
                                onAssignSongToSinger = viewModel::assignSongToSinger,
                                onPlaySong = viewModel::playSong,
                                onPlayAll = viewModel::playAll,
                                onToggleLike = viewModel::toggleLike,
                                onOpenSongOptions = { song -> songForOptionsSheet = song },
                                onToggleSelectSinger = viewModel::toggleSelectSinger,
                                onToggleSelectSong = viewModel::toggleSelectSong
                            )
                        }

                        PrimaryTab.LIKED -> {
                            LikedSongsScreen(
                                uiState = uiState,
                                onPlaySong = { song -> viewModel.playSong(song, uiState.likedSongs) },
                                onPlayAll = { shuffle -> viewModel.playAll(uiState.likedSongs, shuffle) },
                                onToggleLike = viewModel::toggleLike,
                                onOpenSongOptions = { song -> songForOptionsSheet = song },
                                onToggleSelectSong = viewModel::toggleSelectSong
                            )
                        }

                        PrimaryTab.RECORDINGS -> {
                            RecordingsSectionScreen(
                                uiState = uiState,
                                onSelectCategory = viewModel::openRecordingCategory,
                                onSortOrderChange = viewModel::updateRecordingSortOrder,
                                onRequestDeviceScan = requestDeviceScanAction,
                                onImportToCategory = { category ->
                                    targetRecordingCategoryForImport = category
                                    importToRecordingFolderLauncher.launch(arrayOf("audio/*"))
                                },
                                onPlayRecording = viewModel::playSong,
                                onPlayAllInFolder = viewModel::playAll,
                                onToggleLike = viewModel::toggleLike,
                                onOpenRecordingOptions = { rec -> songForOptionsSheet = rec }
                            )
                        }
                    }
                }
            }
        }

        // Expandable Full-Screen Now Playing Studio
        AnimatedVisibility(
            visible = uiState.isNowPlayingExpanded && uiState.playback.currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            FullScreenNowPlayingView(
                playback = uiState.playback,
                onCollapse = { viewModel.setNowPlayingExpanded(false) },
                onTogglePlayPause = viewModel::togglePlayPause,
                onSeekTo = viewModel::seekTo,
                onSkipNext = viewModel::skipNext,
                onSkipPrevious = viewModel::skipPrevious,
                onToggleShuffle = viewModel::toggleShuffle,
                onCycleRepeat = viewModel::cycleRepeatMode,
                onToggleLike = viewModel::toggleLike,
                onOpenSongOptions = { song -> songForOptionsSheet = song },
                onPlayQueueIndex = viewModel::playFromQueueIndex,
                onMoveQueueItem = viewModel::moveQueueItem,
                onRemoveQueueItem = viewModel::removeFromQueue,
                onSetEqualizerEnabled = viewModel::setEqualizerEnabled,
                onSelectEqualizerPreset = viewModel::selectEqualizerPreset,
                onUpdateBandGain = viewModel::updateBandGain,
                onUpdateBassBoost = viewModel::updateBassBoost,
                onSaveCustomTuning = viewModel::saveCustomEqualizerTuning,
                onApplySavedCustomTuning = viewModel::applySavedCustomTuning,
                onDeleteSavedCustomTuning = viewModel::deleteSavedCustomTuning,
                onResetCustomTuningToFlat = viewModel::resetCustomTuningToFlat,
                onSetSleepTimerMinutes = viewModel::setSleepTimerMinutes,
                onSetStopAtEndOfTrack = viewModel::setStopAtEndOfTrack
            )
        }
    }

    // Track Organization Bottom Sheet
    songForOptionsSheet?.let { targetSong ->
        val freshSong = (uiState.songs + uiState.recordings).find { it.id == targetSong.id } ?: targetSong
        TrackOrganizationSheet(
            song = freshSong,
            albums = uiState.albums,
            singers = uiState.singers,
            onDismiss = { songForOptionsSheet = null },
            onPlayNext = { viewModel.addSongNextInQueue(freshSong) },
            onToggleLike = { viewModel.toggleLike(freshSong) },
            onAssignToAlbum = { album -> viewModel.assignSongToAlbum(freshSong, album) },
            onAssignToSinger = { singer -> viewModel.assignSongToSinger(freshSong, singer) },
            onMoveToRecordingFolder = { category ->
                viewModel.moveSongToRecordingFolder(freshSong, category)
            },
            onEditMetadata = { songForMetadataEdit = freshSong },
            onDeleteSong = { viewModel.deleteSong(freshSong) }
        )
    }

    // Edit Song Metadata Dialog
    songForMetadataEdit?.let { targetSong ->
        EditSongMetadataDialog(
            song = targetSong,
            onDismiss = { songForMetadataEdit = null },
            onConfirm = { newTitle, newSinger, newAlbum ->
                viewModel.updateSongMetadata(targetSong, newTitle, newSinger, newAlbum)
            }
        )
    }

    // Create Album Folder Dialog
    if (showCreateAlbumDialog) {
        AlbumFolderDialog(
            existingAlbum = null,
            allSongs = uiState.songs,
            onDismiss = { showCreateAlbumDialog = false },
            onSave = { title, singerName, year, description, colorHex, selectedIds ->
                viewModel.createAlbumFolder(title, singerName, year, description, colorHex, selectedIds)
            }
        )
    }

    // Edit Album Folder Dialog
    albumToEdit?.let { existing ->
        AlbumFolderDialog(
            existingAlbum = existing,
            allSongs = uiState.songs,
            onDismiss = { albumToEdit = null },
            onSave = { title, singerName, year, description, colorHex, _ ->
                viewModel.updateAlbumFolder(existing, title, singerName, year, description, colorHex)
            }
        )
    }

    // Create Singer Folder Dialog
    if (showCreateSingerDialog) {
        SingerFolderDialog(
            existingSinger = null,
            allSongs = uiState.songs,
            onDismiss = { showCreateSingerDialog = false },
            onSave = { name, genre, bio, colorHex, selectedIds ->
                viewModel.createSingerFolder(name, genre, bio, colorHex, selectedIds)
            }
        )
    }

    // Edit Singer Folder Dialog
    singerToEdit?.let { existing ->
        SingerFolderDialog(
            existingSinger = existing,
            allSongs = uiState.songs,
            onDismiss = { singerToEdit = null },
            onSave = { name, genre, bio, colorHex, _ ->
                viewModel.updateSingerFolder(existing, name, genre, bio, colorHex)
            }
        )
    }
}
