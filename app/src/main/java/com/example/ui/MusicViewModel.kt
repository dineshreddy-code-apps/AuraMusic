package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AlbumFolderEntity
import com.example.data.MusicDatabase
import com.example.data.MusicRepository
import com.example.data.RecordingCategory
import com.example.data.SingerFolderEntity
import com.example.data.SongEntity
import com.example.player.AudioPlayerController
import com.example.player.EqualizerPreset
import com.example.player.PlaybackState
import com.example.player.SavedCustomTuning
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PrimaryTab {
    SONGS,
    ALBUMS,
    SINGERS,
    LIKED,
    RECORDINGS
}

enum class SongSortOrder(val label: String) {
    RECENT("Recently Added"),
    TITLE("Title A–Z"),
    SINGER("Singer A–Z"),
    DURATION("Duration")
}

enum class FolderSortOrder(val label: String) {
    DATE_ADDED("Date Added"),
    ALPHABETICAL("Alphabetical"),
    PLAY_COUNT("Play Count")
}

data class MusicUiState(
    val songs: List<SongEntity> = emptyList(),
    val filteredSongs: List<SongEntity> = emptyList(),
    val likedSongs: List<SongEntity> = emptyList(),
    val recordings: List<SongEntity> = emptyList(),
    val albums: List<AlbumFolderEntity> = emptyList(),
    val filteredAlbums: List<AlbumFolderEntity> = emptyList(),
    val singers: List<SingerFolderEntity> = emptyList(),
    val filteredSingers: List<SingerFolderEntity> = emptyList(),
    val playback: PlaybackState = PlaybackState(),
    val selectedTab: PrimaryTab = PrimaryTab.SONGS,
    val selectedAlbumId: Long? = null,
    val selectedSingerId: Long? = null,
    val selectedRecordingCategory: RecordingCategory? = null,
    val searchQuery: String = "",
    val sortOrder: SongSortOrder = SongSortOrder.RECENT,
    val songSortReversed: Boolean = false,
    val albumSortOrder: FolderSortOrder = FolderSortOrder.DATE_ADDED,
    val albumSortReversed: Boolean = false,
    val singerSortOrder: FolderSortOrder = FolderSortOrder.DATE_ADDED,
    val singerSortReversed: Boolean = false,
    val recordingSortOrder: FolderSortOrder = FolderSortOrder.DATE_ADDED,
    val recordingSortReversed: Boolean = false,
    val isNowPlayingExpanded: Boolean = false,
    val isScanningDevice: Boolean = false,
    val statusMessage: String? = null,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val selectedSongIds: Set<Long> = emptySet(),
    val selectedAlbumIds: Set<Long> = emptySet(),
    val selectedSingerIds: Set<Long> = emptySet()
) {
    val isSelectionMode: Boolean
        get() = selectedSongIds.isNotEmpty() || selectedAlbumIds.isNotEmpty() || selectedSingerIds.isNotEmpty()
}

private data class FilterUiConfig(
    val selectedTab: PrimaryTab = PrimaryTab.SONGS,
    val selectedAlbumId: Long? = null,
    val selectedSingerId: Long? = null,
    val selectedRecordingCategory: RecordingCategory? = null,
    val searchQuery: String = "",
    val sortOrder: SongSortOrder = SongSortOrder.RECENT,
    val songSortReversed: Boolean = false,
    val albumSortOrder: FolderSortOrder = FolderSortOrder.DATE_ADDED,
    val albumSortReversed: Boolean = false,
    val singerSortOrder: FolderSortOrder = FolderSortOrder.DATE_ADDED,
    val singerSortReversed: Boolean = false,
    val recordingSortOrder: FolderSortOrder = FolderSortOrder.DATE_ADDED,
    val recordingSortReversed: Boolean = false,
    val isNowPlayingExpanded: Boolean = false,
    val isScanningDevice: Boolean = false,
    val statusMessage: String? = null,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val selectedSongIds: Set<Long> = emptySet(),
    val selectedAlbumIds: Set<Long> = emptySet(),
    val selectedSingerIds: Set<Long> = emptySet()
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MusicRepository
    private val playerController: AudioPlayerController
    private val uiConfig = MutableStateFlow(FilterUiConfig())

    val uiState: StateFlow<MusicUiState>

    init {
        val dao = MusicDatabase.getDatabase(application).musicDao()
        repository = MusicRepository(dao, application)
        playerController = AudioPlayerController.getInstance(application)

        viewModelScope.launch {
            repository.ensureInitialLibrarySeeded()
        }

        viewModelScope.launch {
            combine(repository.allSongs, repository.allRecordings) { songs, recordings ->
                songs to recordings
            }.collect { (songs, recordings) ->
                playerController.syncUpdatedSongs(songs + recordings)
                if (playerController.state.value.currentSong == null && songs.isNotEmpty()) {
                    // Pre-populate mini-player with a music track in paused ready state (never a recording)
                    val first = songs.first()
                    playerController.prepareInitialSong(first, songs)
                }
            }
        }

        val libraryFlow = combine(
            repository.allSongs,
            repository.likedSongs,
            repository.allRecordings,
            repository.allAlbums,
            repository.allSingers
        ) { songs, liked, recordings, albums, singers ->
            LibrarySnapshot(songs, liked, recordings, albums, singers)
        }

        uiState = combine(
            libraryFlow,
            playerController.state,
            uiConfig
        ) { library, playback, config ->
            val query = config.searchQuery.trim().lowercase()
            val searched = if (query.isBlank()) {
                library.songs
            } else {
                library.songs.filter {
                    it.title.lowercase().contains(query) ||
                        it.singerName.lowercase().contains(query) ||
                        it.albumName.lowercase().contains(query)
                }
            }
            val baseSortedSongs = when (config.sortOrder) {
                SongSortOrder.RECENT -> searched.sortedByDescending { it.dateAdded }
                SongSortOrder.TITLE -> searched.sortedBy { it.title.lowercase() }
                SongSortOrder.SINGER -> searched.sortedBy { it.singerName.lowercase() }
                SongSortOrder.DURATION -> searched.sortedByDescending { it.durationMs }
            }
            val sortedSongs = if (config.songSortReversed) baseSortedSongs.reversed() else baseSortedSongs

            // Exclude any album or singer folder that has zero songs in the music library
            val nonEmptyAlbums = library.albums.filter { album ->
                library.songs.any { song ->
                    song.albumId == album.id || song.albumName.equals(album.title, ignoreCase = true)
                }
            }
            val baseSortedAlbums = when (config.albumSortOrder) {
                FolderSortOrder.DATE_ADDED -> nonEmptyAlbums.sortedByDescending { it.createdAt }
                FolderSortOrder.ALPHABETICAL -> nonEmptyAlbums.sortedBy { it.title.lowercase() }
                FolderSortOrder.PLAY_COUNT -> nonEmptyAlbums.sortedByDescending { album ->
                    val tracksPlays = library.songs
                        .filter { it.albumId == album.id || it.albumName.equals(album.title, ignoreCase = true) }
                        .sumOf { it.playCount }
                    album.playCount + tracksPlays
                }
            }
            val sortedAlbums = if (config.albumSortReversed) baseSortedAlbums.reversed() else baseSortedAlbums
            val searchedAlbums = if (query.isBlank()) {
                sortedAlbums
            } else {
                sortedAlbums.filter { album ->
                    album.title.lowercase().contains(query) ||
                        album.singerName.lowercase().contains(query)
                }
            }

            val nonEmptySingers = library.singers.filter { singer ->
                library.songs.any { song ->
                    song.singerId == singer.id || song.singerName.equals(singer.name, ignoreCase = true)
                }
            }
            val baseSortedSingers = when (config.singerSortOrder) {
                FolderSortOrder.DATE_ADDED -> nonEmptySingers.sortedByDescending { it.createdAt }
                FolderSortOrder.ALPHABETICAL -> nonEmptySingers.sortedBy { it.name.lowercase() }
                FolderSortOrder.PLAY_COUNT -> nonEmptySingers.sortedByDescending { singer ->
                    val tracksPlays = library.songs
                        .filter { it.singerId == singer.id || it.singerName.equals(singer.name, ignoreCase = true) }
                        .sumOf { it.playCount }
                    singer.playCount + tracksPlays
                }
            }
            val sortedSingers = if (config.singerSortReversed) baseSortedSingers.reversed() else baseSortedSingers
            val searchedSingers = if (query.isBlank()) {
                sortedSingers
            } else {
                sortedSingers.filter { singer ->
                    singer.name.lowercase().contains(query) ||
                        singer.genreTag.lowercase().contains(query)
                }
            }

            val baseSortedRecordings = when (config.recordingSortOrder) {
                FolderSortOrder.DATE_ADDED -> library.recordings.sortedByDescending { it.dateAdded }
                FolderSortOrder.ALPHABETICAL -> library.recordings.sortedBy { it.title.lowercase() }
                FolderSortOrder.PLAY_COUNT -> library.recordings.sortedByDescending { it.playCount }
            }
            val sortedRecordings = if (config.recordingSortReversed) baseSortedRecordings.reversed() else baseSortedRecordings

            MusicUiState(
                songs = library.songs,
                filteredSongs = sortedSongs,
                likedSongs = library.liked,
                recordings = sortedRecordings,
                albums = sortedAlbums,
                filteredAlbums = searchedAlbums,
                singers = sortedSingers,
                filteredSingers = searchedSingers,
                playback = playback,
                selectedTab = config.selectedTab,
                selectedAlbumId = config.selectedAlbumId?.takeIf { id -> sortedAlbums.any { it.id == id } },
                selectedSingerId = config.selectedSingerId?.takeIf { id -> sortedSingers.any { it.id == id } },
                selectedRecordingCategory = config.selectedRecordingCategory,
                searchQuery = config.searchQuery,
                sortOrder = config.sortOrder,
                songSortReversed = config.songSortReversed,
                albumSortOrder = config.albumSortOrder,
                albumSortReversed = config.albumSortReversed,
                singerSortOrder = config.singerSortOrder,
                singerSortReversed = config.singerSortReversed,
                recordingSortOrder = config.recordingSortOrder,
                recordingSortReversed = config.recordingSortReversed,
                isNowPlayingExpanded = config.isNowPlayingExpanded,
                isScanningDevice = config.isScanningDevice,
                statusMessage = config.statusMessage,
                themeMode = config.themeMode,
                selectedSongIds = config.selectedSongIds,
                selectedAlbumIds = config.selectedAlbumIds,
                selectedSingerIds = config.selectedSingerIds
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MusicUiState()
        )
    }

    // Long-press (>1s) Multi-Selection & Share Controls
    fun toggleSelectSong(songId: Long) {
        uiConfig.update { current ->
            val next = current.selectedSongIds.toMutableSet()
            if (!next.add(songId)) next.remove(songId)
            current.copy(selectedSongIds = next)
        }
    }

    fun toggleSelectAlbum(albumId: Long) {
        uiConfig.update { current ->
            val next = current.selectedAlbumIds.toMutableSet()
            if (!next.add(albumId)) next.remove(albumId)
            current.copy(selectedAlbumIds = next)
        }
    }

    fun toggleSelectSinger(singerId: Long) {
        uiConfig.update { current ->
            val next = current.selectedSingerIds.toMutableSet()
            if (!next.add(singerId)) next.remove(singerId)
            current.copy(selectedSingerIds = next)
        }
    }

    fun selectAllInCurrentView() {
        val state = uiState.value
        uiConfig.update { current ->
            when (state.selectedTab) {
                PrimaryTab.SONGS -> {
                    val ids = state.filteredSongs.map { it.id }.toSet()
                    val allAlreadySelected = ids.isNotEmpty() && ids.all { it in current.selectedSongIds }
                    val updatedSongs = if (allAlreadySelected) {
                        current.selectedSongIds - ids
                    } else {
                        current.selectedSongIds + ids
                    }
                    current.copy(selectedSongIds = updatedSongs)
                }
                PrimaryTab.ALBUMS -> {
                    val activeAlbum = state.albums.find { it.id == state.selectedAlbumId }
                    if (activeAlbum != null) {
                        val ids = state.songs.filter {
                            it.albumId == activeAlbum.id || it.albumName.equals(activeAlbum.title, ignoreCase = true)
                        }.map { it.id }.toSet()
                        val allAlreadySelected = ids.isNotEmpty() && ids.all { it in current.selectedSongIds }
                        val updatedSongs = if (allAlreadySelected) {
                            current.selectedSongIds - ids
                        } else {
                            current.selectedSongIds + ids
                        }
                        current.copy(selectedSongIds = updatedSongs)
                    } else {
                        val ids = state.albums.map { it.id }.toSet()
                        val allAlreadySelected = ids.isNotEmpty() && ids.all { it in current.selectedAlbumIds }
                        val updatedAlbums = if (allAlreadySelected) {
                            current.selectedAlbumIds - ids
                        } else {
                            current.selectedAlbumIds + ids
                        }
                        current.copy(selectedAlbumIds = updatedAlbums)
                    }
                }
                PrimaryTab.SINGERS -> {
                    val activeSinger = state.singers.find { it.id == state.selectedSingerId }
                    if (activeSinger != null) {
                        val ids = state.songs.filter {
                            it.singerId == activeSinger.id || it.singerName.equals(activeSinger.name, ignoreCase = true)
                        }.map { it.id }.toSet()
                        val allAlreadySelected = ids.isNotEmpty() && ids.all { it in current.selectedSongIds }
                        val updatedSongs = if (allAlreadySelected) {
                            current.selectedSongIds - ids
                        } else {
                            current.selectedSongIds + ids
                        }
                        current.copy(selectedSongIds = updatedSongs)
                    } else {
                        val ids = state.singers.map { it.id }.toSet()
                        val allAlreadySelected = ids.isNotEmpty() && ids.all { it in current.selectedSingerIds }
                        val updatedSingers = if (allAlreadySelected) {
                            current.selectedSingerIds - ids
                        } else {
                            current.selectedSingerIds + ids
                        }
                        current.copy(selectedSingerIds = updatedSingers)
                    }
                }
                PrimaryTab.LIKED -> {
                    val ids = state.likedSongs.map { it.id }.toSet()
                    val allAlreadySelected = ids.isNotEmpty() && ids.all { it in current.selectedSongIds }
                    val updatedSongs = if (allAlreadySelected) {
                        current.selectedSongIds - ids
                    } else {
                        current.selectedSongIds + ids
                    }
                    current.copy(selectedSongIds = updatedSongs)
                }
                PrimaryTab.RECORDINGS -> {
                    val cat = state.selectedRecordingCategory
                    val recs = if (cat != null) {
                        state.recordings.filter { it.recordingCategory == cat.id }
                    } else {
                        state.recordings
                    }
                    val ids = recs.map { it.id }.toSet()
                    val allAlreadySelected = ids.isNotEmpty() && ids.all { it in current.selectedSongIds }
                    val updatedSongs = if (allAlreadySelected) {
                        current.selectedSongIds - ids
                    } else {
                        current.selectedSongIds + ids
                    }
                    current.copy(selectedSongIds = updatedSongs)
                }
            }
        }
    }

    fun clearMultiSelection() {
        uiConfig.update {
            it.copy(
                selectedSongIds = emptySet(),
                selectedAlbumIds = emptySet(),
                selectedSingerIds = emptySet()
            )
        }
    }

    private data class LibrarySnapshot(
        val songs: List<SongEntity>,
        val liked: List<SongEntity>,
        val recordings: List<SongEntity>,
        val albums: List<AlbumFolderEntity>,
        val singers: List<SingerFolderEntity>
    )

    // Navigation & Filtering
    fun selectTab(tab: PrimaryTab) {
        uiConfig.update {
            it.copy(
                selectedTab = tab,
                selectedAlbumId = null,
                selectedSingerId = null,
                selectedRecordingCategory = null,
                searchQuery = ""
            )
        }
    }

    fun openAlbumDetail(albumId: Long?) {
        uiConfig.update {
            it.copy(
                selectedAlbumId = albumId,
                searchQuery = ""
            )
        }
    }

    fun openSingerDetail(singerId: Long?) {
        uiConfig.update {
            it.copy(
                selectedSingerId = singerId,
                searchQuery = ""
            )
        }
    }

    fun openRecordingCategory(category: RecordingCategory?) {
        uiConfig.update { it.copy(selectedRecordingCategory = category) }
    }

    fun navigateToAlbumFromSearch(albumId: Long) {
        val state = uiState.value
        val album = state.albums.find { it.id == albumId }
        uiConfig.update {
            it.copy(
                selectedTab = PrimaryTab.ALBUMS,
                selectedAlbumId = albumId,
                selectedSingerId = null,
                selectedRecordingCategory = null,
                searchQuery = ""
            )
        }
        if (album != null) {
            val albumSongs = state.songs.filter {
                it.albumId == album.id || it.albumName.equals(album.title, ignoreCase = true)
            }
            if (albumSongs.isNotEmpty()) {
                val firstSong = albumSongs.first()
                playSong(firstSong, albumSongs)
            }
        }
    }

    fun navigateToSingerFromSearch(singerId: Long) {
        val state = uiState.value
        val singer = state.singers.find { it.id == singerId }
        uiConfig.update {
            it.copy(
                selectedTab = PrimaryTab.SINGERS,
                selectedSingerId = singerId,
                selectedAlbumId = null,
                selectedRecordingCategory = null,
                searchQuery = ""
            )
        }
        if (singer != null) {
            val singerSongs = state.songs.filter {
                it.singerId == singer.id || it.singerName.equals(singer.name, ignoreCase = true)
            }
            if (singerSongs.isNotEmpty()) {
                val firstSong = singerSongs.first()
                playSong(firstSong, singerSongs)
            }
        }
    }

    fun setNowPlayingExpanded(expanded: Boolean) {
        uiConfig.update { it.copy(isNowPlayingExpanded = expanded) }
    }

    fun updateSearchQuery(query: String) {
        uiConfig.update { it.copy(searchQuery = query) }
    }

    fun updateSortOrder(order: SongSortOrder) {
        uiConfig.update {
            if (it.sortOrder == order) {
                it.copy(songSortReversed = !it.songSortReversed)
            } else {
                it.copy(sortOrder = order, songSortReversed = false)
            }
        }
    }

    fun updateAlbumSortOrder(order: FolderSortOrder) {
        uiConfig.update {
            if (it.albumSortOrder == order) {
                it.copy(albumSortReversed = !it.albumSortReversed)
            } else {
                it.copy(albumSortOrder = order, albumSortReversed = false)
            }
        }
    }

    fun updateSingerSortOrder(order: FolderSortOrder) {
        uiConfig.update {
            if (it.singerSortOrder == order) {
                it.copy(singerSortReversed = !it.singerSortReversed)
            } else {
                it.copy(singerSortOrder = order, singerSortReversed = false)
            }
        }
    }

    fun updateRecordingSortOrder(order: FolderSortOrder) {
        uiConfig.update {
            if (it.recordingSortOrder == order) {
                it.copy(recordingSortReversed = !it.recordingSortReversed)
            } else {
                it.copy(recordingSortOrder = order, recordingSortReversed = false)
            }
        }
    }

    fun cycleThemeMode() {
        uiConfig.update {
            val next = when (it.themeMode) {
                AppThemeMode.SYSTEM -> AppThemeMode.DARK
                AppThemeMode.DARK -> AppThemeMode.LIGHT
                AppThemeMode.LIGHT -> AppThemeMode.SYSTEM
            }
            it.copy(themeMode = next)
        }
    }

    fun clearStatusMessage() {
        uiConfig.update { it.copy(statusMessage = null) }
    }

    // Device Media Scan & Audio File Import
    fun scanDeviceStorage() {
        viewModelScope.launch {
            uiConfig.update { it.copy(isScanningDevice = true) }
            val added = repository.scanAndImportDeviceSongs()
            val message = if (added > 0) {
                "Imported $added new offline audio item${if (added == 1) "" else "s"} (songs & recordings separated)."
            } else {
                "Device scan complete. Music and Recording folders are up to date."
            }
            uiConfig.update {
                it.copy(
                    isScanningDevice = false,
                    statusMessage = message
                )
            }
        }
    }

    fun importAudioFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            uiConfig.update { it.copy(isScanningDevice = true) }
            val added = repository.importAudioUris(uris)
            val message = if (added > 0) {
                "Added $added audio item${if (added == 1) "" else "s"} (automatically sorted into Music or Recordings)."
            } else {
                "No new audio tracks found or files are already in your library."
            }
            uiConfig.update {
                it.copy(
                    isScanningDevice = false,
                    statusMessage = message
                )
            }
        }
    }

    fun importRecordingsToCategory(uris: List<Uri>, category: RecordingCategory) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            uiConfig.update { it.copy(isScanningDevice = true) }
            val added = repository.importAudioUrisToRecordingFolder(uris, category)
            val message = if (added > 0) {
                "Added $added recording${if (added == 1) "" else "s"} to \"${category.folderTitle}\"."
            } else {
                "Selected recordings are already in \"${category.folderTitle}\"."
            }
            uiConfig.update {
                it.copy(
                    isScanningDevice = false,
                    statusMessage = message
                )
            }
        }
    }

    fun importZipArchives(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            uiConfig.update { it.copy(isScanningDevice = true) }
            val added = repository.importZipArchives(uris)
            val message = if (added > 0) {
                "Extracted & organized $added audio file${if (added == 1) "" else "s"} from ZIP archive."
            } else {
                "No new audio files (.mp3, .wav, .flac, .m4a, .ogg) found inside the selected ZIP."
            }
            uiConfig.update {
                it.copy(
                    isScanningDevice = false,
                    statusMessage = message
                )
            }
        }
    }

    fun restoreStudioSamples() {
        viewModelScope.launch {
            val added = repository.seedStudioAcousticSamples()
            val msg = if (added > 0) {
                "Restored $added studio acoustic tracks."
            } else {
                "All studio acoustic tracks are already in your library."
            }
            uiConfig.update { it.copy(statusMessage = msg) }
        }
    }

    // Playback Controls & Continuous Next-Album / Next-Singer / Liked Queue Builder
    private fun resolveContinuationQueue(
        primarySongs: List<SongEntity>,
        targetSong: SongEntity
    ): Pair<List<SongEntity>, String?> {
        if (targetSong.recordingCategory != null) {
            return emptyList<SongEntity>() to null
        }
        val state = uiState.value
        val primaryIds = primarySongs.map { it.id }.toSet()

        // 1. Check if playing from an Album context
        val isAlbumContext = state.selectedTab == PrimaryTab.ALBUMS ||
            (primarySongs.isNotEmpty() && primarySongs.all {
                (targetSong.albumId != null && it.albumId == targetSong.albumId) ||
                    it.albumName.equals(targetSong.albumName, ignoreCase = true)
            } && primarySongs.size < state.songs.size)

        if (isAlbumContext && state.albums.size > 1) {
            val albums = state.albums
            val activeAlbumIdx = albums.indexOfFirst {
                (state.selectedAlbumId != null && it.id == state.selectedAlbumId) ||
                    (targetSong.albumId != null && it.id == targetSong.albumId) ||
                    it.title.equals(targetSong.albumName, ignoreCase = true)
            }.coerceAtLeast(0)

            val nextAlbums = (1 until albums.size).map { offset ->
                albums[(activeAlbumIdx + offset) % albums.size]
            }
            val seenIds = primaryIds.toMutableSet()
            val continuation = mutableListOf<SongEntity>()
            for (nextAlbum in nextAlbums) {
                val albumTracks = state.songs.filter {
                    it.recordingCategory == null &&
                        (it.albumId == nextAlbum.id || it.albumName.equals(nextAlbum.title, ignoreCase = true))
                }
                for (track in albumTracks) {
                    if (seenIds.add(track.id)) {
                        continuation.add(track)
                    }
                }
            }
            // Append any remaining library songs not in those albums
            for (song in state.songs) {
                if (song.recordingCategory == null && seenIds.add(song.id)) {
                    continuation.add(song)
                }
            }
            val firstNextAlbumTitle = nextAlbums.firstOrNull { album ->
                state.songs.any { it.albumId == album.id || it.albumName.equals(album.title, ignoreCase = true) }
            }?.title
            val label = if (firstNextAlbumTitle != null) {
                "Auto-queued next album: \"$firstNextAlbumTitle\" (Repeat Off)"
            } else {
                "Auto-queued next albums (Repeat Off)"
            }
            return continuation to label
        }

        // 2. Check if playing from a Singer context
        val isSingerContext = state.selectedTab == PrimaryTab.SINGERS ||
            (primarySongs.isNotEmpty() && primarySongs.all {
                (targetSong.singerId != null && it.singerId == targetSong.singerId) ||
                    it.singerName.equals(targetSong.singerName, ignoreCase = true)
            } && primarySongs.size < state.songs.size)

        if (isSingerContext && state.singers.size > 1) {
            val singers = state.singers
            val activeSingerIdx = singers.indexOfFirst {
                (state.selectedSingerId != null && it.id == state.selectedSingerId) ||
                    (targetSong.singerId != null && it.id == targetSong.singerId) ||
                    it.name.equals(targetSong.singerName, ignoreCase = true)
            }.coerceAtLeast(0)

            val nextSingers = (1 until singers.size).map { offset ->
                singers[(activeSingerIdx + offset) % singers.size]
            }
            val seenIds = primaryIds.toMutableSet()
            val continuation = mutableListOf<SongEntity>()
            for (nextSinger in nextSingers) {
                val singerTracks = state.songs.filter {
                    it.recordingCategory == null &&
                        (it.singerId == nextSinger.id || it.singerName.equals(nextSinger.name, ignoreCase = true))
                }
                for (track in singerTracks) {
                    if (seenIds.add(track.id)) {
                        continuation.add(track)
                    }
                }
            }
            for (song in state.songs) {
                if (song.recordingCategory == null && seenIds.add(song.id)) {
                    continuation.add(song)
                }
            }
            val firstNextSingerName = nextSingers.firstOrNull { singer ->
                state.songs.any { it.singerId == singer.id || it.singerName.equals(singer.name, ignoreCase = true) }
            }?.name
            val label = if (firstNextSingerName != null) {
                "Auto-queued next singer: \"$firstNextSingerName\" (Repeat Off)"
            } else {
                "Auto-queued next singers (Repeat Off)"
            }
            return continuation to label
        }

        // 3. Check if playing from Liked Songs context
        val isLikedContext = state.selectedTab == PrimaryTab.LIKED ||
            (primarySongs.isNotEmpty() && primarySongs.all { it.isLiked } && primarySongs.size < state.songs.size)

        if (isLikedContext) {
            val seenIds = primaryIds.toMutableSet()
            val continuation = mutableListOf<SongEntity>()
            // Order remaining songs by the user's sorted albums/library for seamless continuous playback
            for (album in state.albums) {
                val albumTracks = state.songs.filter {
                    it.recordingCategory == null &&
                        (it.albumId == album.id || it.albumName.equals(album.title, ignoreCase = true))
                }
                for (track in albumTracks) {
                    if (seenIds.add(track.id)) {
                        continuation.add(track)
                    }
                }
            }
            for (song in state.songs) {
                if (song.recordingCategory == null && seenIds.add(song.id)) {
                    continuation.add(song)
                }
            }
            return continuation to "Auto-queued next library albums after Liked Songs (Repeat Off)"
        }

        return emptyList<SongEntity>() to null
    }

    fun playSong(song: SongEntity, queueContext: List<SongEntity> = uiState.value.filteredSongs) {
        val wasSearching = uiConfig.value.searchQuery.isNotBlank()
        if (wasSearching) {
            // Exit global search once any song is selected for playing
            uiConfig.update { it.copy(searchQuery = "") }
        }
        val currentPlayback = uiState.value.playback
        // If clicking on the song that is already playing, take no action
        if (currentPlayback.currentSong?.id == song.id && currentPlayback.isPlaying) {
            return
        }
        val effectiveContext = if (wasSearching && uiState.value.selectedTab == PrimaryTab.SONGS) {
            uiState.value.songs.ifEmpty { listOf(song) }
        } else {
            queueContext.ifEmpty { listOf(song) }
        }
        val (continuation, label) = resolveContinuationQueue(effectiveContext, song)
        playerController.playSong(
            song = song,
            contextQueue = effectiveContext,
            continuationQueue = continuation,
            continuationLabel = label
        )
        viewModelScope.launch {
            repository.recordSongPlayed(song)
        }
    }

    fun playAll(songs: List<SongEntity>, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        if (uiConfig.value.searchQuery.isNotBlank()) {
            uiConfig.update { it.copy(searchQuery = "") }
        }
        if (shuffle != uiState.value.playback.shuffleEnabled) {
            playerController.toggleShuffle()
        }
        val target = if (shuffle) songs.random() else songs.first()
        val (continuation, label) = resolveContinuationQueue(songs, target)
        playerController.playSong(
            song = target,
            contextQueue = songs,
            continuationQueue = continuation,
            continuationLabel = label
        )
        viewModelScope.launch {
            repository.recordSongPlayed(target)
        }
    }

    fun togglePlayPause() = playerController.togglePlayPause()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
    fun skipNext() = playerController.skipNext()
    fun skipPrevious() = playerController.skipPrevious()
    fun toggleShuffle() = playerController.toggleShuffle()
    fun cycleRepeatMode() = playerController.cycleRepeatMode()
    fun playFromQueueIndex(index: Int) = playerController.playFromQueueIndex(index)
    fun moveQueueItem(from: Int, to: Int) = playerController.moveQueueItem(from, to)
    fun removeFromQueue(index: Int) = playerController.removeFromQueue(index)

    fun addSongNextInQueue(song: SongEntity) {
        playerController.addSongNextInQueue(song)
        uiConfig.update { it.copy(statusMessage = "\"${song.title}\" will play next") }
    }

    // Equalizer, Custom Tunings & Sleep Timer
    fun setEqualizerEnabled(enabled: Boolean) = playerController.setEqualizerEnabled(enabled)
    fun selectEqualizerPreset(preset: EqualizerPreset) = playerController.selectEqualizerPreset(preset)
    fun updateBandGain(bandIndex: Int, gainDb: Float) = playerController.updateBandGain(bandIndex, gainDb)
    fun updateBassBoost(strength: Int) = playerController.updateBassBoost(strength)

    fun saveCustomEqualizerTuning(name: String) {
        val saved = playerController.saveCustomTuning(name)
        uiConfig.update {
            it.copy(statusMessage = "Saved custom tuning \"${saved.name}\" in Equalizer & Studio FX")
        }
    }

    fun applySavedCustomTuning(tuning: SavedCustomTuning) {
        playerController.applySavedCustomTuning(tuning)
        uiConfig.update {
            it.copy(statusMessage = "Applied custom tuning \"${tuning.name}\"")
        }
    }

    fun deleteSavedCustomTuning(tuningId: String) {
        playerController.deleteSavedCustomTuning(tuningId)
        uiConfig.update {
            it.copy(statusMessage = "Removed saved custom tuning")
        }
    }

    fun resetCustomTuningToFlat() {
        playerController.resetCustomTuningToFlat()
        uiConfig.update {
            it.copy(statusMessage = "Reset custom tuning bands to 0 dB")
        }
    }

    fun setSleepTimerMinutes(minutes: Int?) {
        playerController.setSleepTimerMinutes(minutes)
        val msg = if (minutes != null) "Sleep timer set for $minutes min" else "Sleep timer turned off"
        uiConfig.update { it.copy(statusMessage = msg) }
    }

    fun setStopAtEndOfTrack(enabled: Boolean) {
        playerController.setStopAtEndOfTrack(enabled)
        val msg = if (enabled) "Playback will pause after current song" else "Sleep timer turned off"
        uiConfig.update { it.copy(statusMessage = msg) }
    }

    // Track, Album Folder, Singer Folder, and Recording Folder Organization
    fun toggleLike(song: SongEntity) {
        viewModelScope.launch {
            repository.toggleSongLike(song)
        }
    }

    fun moveSongToRecordingFolder(song: SongEntity, category: RecordingCategory?) {
        viewModelScope.launch {
            repository.moveSongToRecordingCategory(song, category)
            val destination = category?.folderTitle ?: "Music Library"
            uiConfig.update { it.copy(statusMessage = "Moved \"${song.title}\" to $destination") }
        }
    }

    fun updateSongMetadata(song: SongEntity, title: String, singer: String, album: String) {
        viewModelScope.launch {
            repository.updateSongMetadata(song, title, singer, album)
            uiConfig.update { it.copy(statusMessage = "Updated \"${title.trim()}\"") }
        }
    }

    fun assignSongToAlbum(song: SongEntity, album: AlbumFolderEntity) {
        viewModelScope.launch {
            repository.assignSongToAlbum(song.id, album)
            uiConfig.update { it.copy(statusMessage = "Moved \"${song.title}\" to Album \"${album.title}\"") }
        }
    }

    fun assignSongToSinger(song: SongEntity, singer: SingerFolderEntity) {
        viewModelScope.launch {
            repository.assignSongToSinger(song.id, singer)
            uiConfig.update { it.copy(statusMessage = "Assigned \"${song.title}\" to Singer \"${singer.name}\"") }
        }
    }

    fun deleteSong(song: SongEntity) {
        viewModelScope.launch {
            repository.deleteSong(song)
            uiConfig.update { it.copy(statusMessage = "Removed \"${song.title}\" from library") }
        }
    }

    fun createAlbumFolder(
        title: String,
        singerName: String,
        releaseYear: Int,
        description: String,
        coverColorHex: Long,
        initialSongIds: List<Long>
    ) {
        viewModelScope.launch {
            repository.createCustomAlbum(
                title = title,
                singerName = singerName,
                releaseYear = releaseYear,
                description = description,
                coverColorHex = coverColorHex,
                initialSongIds = initialSongIds
            )
            uiConfig.update { it.copy(statusMessage = "Created Album folder \"${title.trim()}\"") }
        }
    }

    fun updateAlbumFolder(
        album: AlbumFolderEntity,
        title: String,
        singerName: String,
        releaseYear: Int,
        description: String,
        coverColorHex: Long
    ) {
        viewModelScope.launch {
            repository.updateAlbumFolder(album, title, singerName, releaseYear, description, coverColorHex)
            uiConfig.update { it.copy(statusMessage = "Updated Album \"${title.trim()}\"") }
        }
    }

    fun deleteAlbumFolder(album: AlbumFolderEntity) {
        viewModelScope.launch {
            if (uiConfig.value.selectedAlbumId == album.id) {
                uiConfig.update { it.copy(selectedAlbumId = null) }
            }
            repository.deleteAlbumFolder(album)
            uiConfig.update { it.copy(statusMessage = "Deleted Album folder \"${album.title}\"") }
        }
    }

    fun createSingerFolder(
        name: String,
        genreTag: String,
        bio: String,
        avatarColorHex: Long,
        initialSongIds: List<Long>
    ) {
        viewModelScope.launch {
            repository.createCustomSinger(
                name = name,
                genreTag = genreTag,
                bio = bio,
                avatarColorHex = avatarColorHex,
                initialSongIds = initialSongIds
            )
            uiConfig.update { it.copy(statusMessage = "Created Singer folder \"${name.trim()}\"") }
        }
    }

    fun updateSingerFolder(
        singer: SingerFolderEntity,
        name: String,
        genreTag: String,
        bio: String,
        avatarColorHex: Long
    ) {
        viewModelScope.launch {
            repository.updateSingerFolder(singer, name, genreTag, bio, avatarColorHex)
            uiConfig.update { it.copy(statusMessage = "Updated Singer \"${name.trim()}\"") }
        }
    }

    fun deleteSingerFolder(singer: SingerFolderEntity) {
        viewModelScope.launch {
            if (uiConfig.value.selectedSingerId == singer.id) {
                uiConfig.update { it.copy(selectedSingerId = null) }
            }
            repository.deleteSingerFolder(singer)
            uiConfig.update { it.copy(statusMessage = "Deleted Singer folder \"${singer.name}\"") }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Playback continues in background via MusicPlaybackService and singleton AudioPlayerController
    }
}
