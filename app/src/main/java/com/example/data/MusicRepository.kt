package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow

class MusicRepository(
    private val dao: MusicDao,
    private val appContext: Context
) {
    val allSongs: Flow<List<SongEntity>> = dao.getAllSongs()
    val likedSongs: Flow<List<SongEntity>> = dao.getLikedSongs()
    val allRecordings: Flow<List<SongEntity>> = dao.getAllRecordings()
    val allAlbums: Flow<List<AlbumFolderEntity>> = dao.getAllAlbums()
    val allSingers: Flow<List<SingerFolderEntity>> = dao.getAllSingers()

    suspend fun ensureInitialLibrarySeeded() {
        reclassifyExistingSongsIfNeeded()
        val currentCount = dao.getSongCount()
        if (currentCount == 0) {
            seedStudioAcousticSamples()
        }
        val recordingCount = dao.getRecordingCount()
        if (recordingCount == 0) {
            seedDefaultRecordingSamples()
        }
        pruneEmptyAlbumsAndSingers()
    }

    private suspend fun reclassifyExistingSongsIfNeeded() {
        val snapshot = dao.getAllSongsSnapshot()
        for (song in snapshot) {
            if (song.sourceType == "STUDIO_SAMPLE" || song.sourceType == "RECORDING_SAMPLE") continue
            if (song.recordingCategory != null) continue
            val detected = DeviceAudioScanner.detectRecordingCategory(
                title = song.title,
                artist = song.singerName,
                album = song.albumName,
                pathOrFileName = song.uriString,
                isMusicFlag = 1
            )
            if (detected != null) {
                dao.updateSongRecordingCategory(
                    songId = song.id,
                    category = detected.id,
                    albumId = null,
                    singerId = null,
                    albumName = detected.folderTitle,
                    colorHex = detected.colorHex
                )
            }
        }
        pruneEmptyAlbumsAndSingers()
    }

    suspend fun pruneEmptyAlbumsAndSingers() {
        val musicSongs = dao.getAllSongsSnapshot().filter { it.recordingCategory == null }
        val albums = dao.getAllAlbumsSnapshot()
        for (album in albums) {
            val hasSongs = musicSongs.any {
                it.albumId == album.id || it.albumName.equals(album.title, ignoreCase = true)
            }
            if (!hasSongs) {
                dao.deleteAlbum(album)
            }
        }
        val singers = dao.getAllSingersSnapshot()
        for (singer in singers) {
            val hasSongs = musicSongs.any {
                it.singerId == singer.id || it.singerName.equals(singer.name, ignoreCase = true)
            }
            if (!hasSongs) {
                dao.deleteSinger(singer)
            }
        }
    }

    suspend fun seedDefaultRecordingSamples(): Int {
        var addedCount = 0
        val now = System.currentTimeMillis()
        StudioAudioGenerator.defaultRecordingSpecs.forEachIndexed { index, spec ->
            val (uriString, durationMs) = StudioAudioGenerator.ensureRecordingWavFile(appContext, spec)
            val existing = dao.getSongByUri(uriString)
            if (existing != null) return@forEachIndexed

            val staggeredTime = now - (index * 2_400_000L)
            dao.insertSong(
                SongEntity(
                    title = spec.title,
                    singerName = spec.sourceLabel,
                    albumName = spec.folderCategory.folderTitle,
                    albumId = null,
                    singerId = null,
                    uriString = uriString,
                    durationMs = durationMs,
                    isLiked = false,
                    colorHex = spec.folderCategory.colorHex,
                    trackNumber = index + 1,
                    playCount = 1,
                    sourceType = "RECORDING_SAMPLE",
                    dateAdded = staggeredTime,
                    recordingCategory = spec.folderCategory.id
                )
            )
            addedCount++
        }
        return addedCount
    }

    suspend fun seedStudioAcousticSamples(): Int {
        var addedCount = 0
        val defaultPlayCounts = listOf(18, 9, 24, 11, 31, 14)
        val now = System.currentTimeMillis()
        StudioAudioGenerator.defaultStudioSpecs.forEachIndexed { index, spec ->
            val (uriString, durationMs) = StudioAudioGenerator.ensureWavFile(appContext, spec)
            val existing = dao.getSongByUri(uriString)
            if (existing != null) return@forEachIndexed

            val initialPlays = defaultPlayCounts.getOrElse(index) { 5 }
            val staggeredTime = now - (index * 3_600_000L)

            val singerId = getOrCreateSingerId(
                name = spec.singerName,
                genreTag = spec.singerGenre,
                bio = spec.singerBio,
                colorHex = spec.singerColorHex,
                isCustom = false,
                createdAt = staggeredTime
            )
            val albumId = getOrCreateAlbumId(
                title = spec.albumTitle,
                singerName = spec.singerName,
                releaseYear = spec.albumYear,
                description = spec.albumDescription,
                colorHex = spec.albumColorHex,
                isCustom = false,
                createdAt = staggeredTime
            )

            dao.insertSong(
                SongEntity(
                    title = spec.title,
                    singerName = spec.singerName,
                    albumName = spec.albumTitle,
                    albumId = albumId,
                    singerId = singerId,
                    uriString = uriString,
                    durationMs = durationMs,
                    isLiked = spec.isLikedByDefault,
                    colorHex = spec.albumColorHex,
                    trackNumber = spec.trackNumber,
                    playCount = initialPlays,
                    sourceType = "STUDIO_SAMPLE",
                    dateAdded = staggeredTime,
                    recordingCategory = null
                )
            )
            addedCount++
        }
        return addedCount
    }

    suspend fun recordSongPlayed(song: SongEntity) {
        dao.incrementSongPlayCount(song.id)
        if (song.recordingCategory == null) {
            song.albumId?.let { dao.incrementAlbumPlayCount(it) }
            song.singerId?.let { dao.incrementSingerPlayCount(it) }
        }
    }

    suspend fun scanAndImportDeviceSongs(): Int {
        reclassifyExistingSongsIfNeeded()
        val scanned = DeviceAudioScanner.scanDeviceMediaStore(appContext)
        return ingestScannedItems(scanned)
    }

    suspend fun importAudioUris(uris: List<Uri>): Int {
        val parsed = DeviceAudioScanner.parseImportedUris(appContext, uris)
        return ingestScannedItems(parsed)
    }

    suspend fun importAudioUrisToRecordingFolder(uris: List<Uri>, category: RecordingCategory): Int {
        val parsed = DeviceAudioScanner.parseImportedUris(
            context = appContext,
            uris = uris,
            forcedRecordingCategory = category
        )
        return ingestScannedItems(parsed)
    }

    suspend fun importZipArchives(uris: List<Uri>): Int {
        val extracted = DeviceAudioScanner.extractAudioFromZipUris(appContext, uris)
        return ingestScannedItems(extracted)
    }

    private suspend fun ingestScannedItems(items: List<ScannedAudioItem>): Int {
        var newlyAdded = 0
        for (item in items) {
            val existing = dao.getSongByUri(item.uriString)
            if (existing != null) {
                // If existing track was reclassified as a recording or forced into a recording folder, update its category
                if (item.recordingCategory != null && existing.recordingCategory != item.recordingCategory) {
                    val cat = RecordingCategory.fromId(item.recordingCategory)
                    dao.updateSongRecordingCategory(
                        songId = existing.id,
                        category = item.recordingCategory,
                        albumId = null,
                        singerId = null,
                        albumName = cat?.folderTitle ?: item.albumName,
                        colorHex = cat?.colorHex ?: item.colorHex
                    )
                    newlyAdded++
                    continue
                }
                // If existing song lacked extracted artwork and we now extracted it, update it
                if (existing.artworkPath.isNullOrBlank() && !item.artworkPath.isNullOrBlank()) {
                    dao.updateSong(existing.copy(artworkPath = item.artworkPath, colorHex = item.colorHex))
                    if (existing.recordingCategory == null) {
                        existing.albumId?.let { dao.updateAlbumArtworkIfBlank(it, item.artworkPath, item.colorHex) }
                        existing.singerId?.let { dao.updateSingerArtworkIfBlank(it, item.artworkPath) }
                    }
                }
                continue
            }

            if (item.recordingCategory != null) {
                val categoryEnum = RecordingCategory.fromId(item.recordingCategory)
                dao.insertSong(
                    SongEntity(
                        title = item.title,
                        singerName = item.singerName,
                        albumName = categoryEnum?.folderTitle ?: item.albumName,
                        albumId = null,
                        singerId = null,
                        uriString = item.uriString,
                        durationMs = item.durationMs,
                        isLiked = false,
                        colorHex = categoryEnum?.colorHex ?: item.colorHex,
                        artworkPath = item.artworkPath,
                        trackNumber = item.trackNumber,
                        sourceType = item.sourceType,
                        recordingCategory = item.recordingCategory
                    )
                )
                newlyAdded++
                continue
            }

            val singerId = getOrCreateSingerId(
                name = item.singerName,
                genreTag = "Local Artist",
                bio = "Imported from offline device storage.",
                colorHex = item.colorHex,
                artworkPath = item.artworkPath,
                isCustom = false
            )
            val albumId = getOrCreateAlbumId(
                title = item.albumName,
                singerName = item.singerName,
                releaseYear = 2026,
                description = "Offline album folder indexed from device storage.",
                colorHex = item.colorHex,
                artworkPath = item.artworkPath,
                isCustom = false
            )

            dao.insertSong(
                SongEntity(
                    title = item.title,
                    singerName = item.singerName,
                    albumName = item.albumName,
                    albumId = albumId,
                    singerId = singerId,
                    uriString = item.uriString,
                    durationMs = item.durationMs,
                    isLiked = false,
                    colorHex = item.colorHex,
                    artworkPath = item.artworkPath,
                    trackNumber = item.trackNumber,
                    sourceType = item.sourceType,
                    recordingCategory = null
                )
            )
            newlyAdded++
        }
        pruneEmptyAlbumsAndSingers()
        return newlyAdded
    }

    private suspend fun getOrCreateSingerId(
        name: String,
        genreTag: String,
        bio: String,
        colorHex: Long,
        artworkPath: String? = null,
        isCustom: Boolean,
        createdAt: Long = System.currentTimeMillis()
    ): Long {
        val existing = dao.getSingerByName(name)
        if (existing != null) {
            if (!artworkPath.isNullOrBlank() && existing.artworkPath.isNullOrBlank()) {
                dao.updateSingerArtworkIfBlank(existing.id, artworkPath)
            }
            return existing.id
        }
        return dao.insertSinger(
            SingerFolderEntity(
                name = name,
                genreTag = genreTag,
                bio = bio,
                avatarColorHex = colorHex,
                artworkPath = artworkPath,
                isCustom = isCustom,
                createdAt = createdAt
            )
        )
    }

    private suspend fun getOrCreateAlbumId(
        title: String,
        singerName: String,
        releaseYear: Int,
        description: String,
        colorHex: Long,
        artworkPath: String? = null,
        isCustom: Boolean,
        createdAt: Long = System.currentTimeMillis()
    ): Long {
        val existing = dao.getAlbumByTitle(title)
        if (existing != null) {
            if (!artworkPath.isNullOrBlank() && existing.artworkPath.isNullOrBlank()) {
                dao.updateAlbumArtworkIfBlank(existing.id, artworkPath, colorHex)
            }
            return existing.id
        }
        return dao.insertAlbum(
            AlbumFolderEntity(
                title = title,
                singerName = singerName,
                releaseYear = releaseYear,
                coverColorHex = colorHex,
                artworkPath = artworkPath,
                description = description,
                isCustom = isCustom,
                createdAt = createdAt
            )
        )
    }

    suspend fun toggleSongLike(song: SongEntity) {
        dao.setSongLiked(song.id, !song.isLiked)
    }

    suspend fun moveSongToRecordingCategory(song: SongEntity, category: RecordingCategory?) {
        if (category != null) {
            dao.updateSongRecordingCategory(
                songId = song.id,
                category = category.id,
                albumId = null,
                singerId = null,
                albumName = category.folderTitle,
                colorHex = category.colorHex
            )
        } else {
            val restoredAlbumName = song.albumName.takeIf { name ->
                RecordingCategory.entries.none { it.folderTitle.equals(name, ignoreCase = true) }
            } ?: "Offline Singles"
            val singerId = getOrCreateSingerId(
                name = song.singerName,
                genreTag = "Local Artist",
                bio = "Restored to Music Library.",
                colorHex = song.colorHex,
                artworkPath = song.artworkPath,
                isCustom = false
            )
            val albumId = getOrCreateAlbumId(
                title = restoredAlbumName,
                singerName = song.singerName,
                releaseYear = 2026,
                description = "Restored to Music Library.",
                colorHex = song.colorHex,
                artworkPath = song.artworkPath,
                isCustom = false
            )
            dao.updateSongRecordingCategory(
                songId = song.id,
                category = null,
                albumId = albumId,
                singerId = singerId,
                albumName = restoredAlbumName,
                colorHex = song.colorHex
            )
        }
        pruneEmptyAlbumsAndSingers()
    }

    suspend fun updateSongMetadata(
        song: SongEntity,
        newTitle: String,
        newSingerName: String,
        newAlbumName: String
    ) {
        if (song.recordingCategory != null) {
            dao.updateSong(
                song.copy(
                    title = newTitle.trim().ifBlank { song.title },
                    singerName = newSingerName.trim().ifBlank { song.singerName },
                    albumName = newAlbumName.trim().ifBlank { song.albumName }
                )
            )
            return
        }
        val singerId = getOrCreateSingerId(
            name = newSingerName,
            genreTag = "Custom Artist",
            bio = "Organized in Aura Music.",
            colorHex = song.colorHex,
            isCustom = true
        )
        val albumId = getOrCreateAlbumId(
            title = newAlbumName,
            singerName = newSingerName,
            releaseYear = 2026,
            description = "Organized in Aura Music.",
            colorHex = song.colorHex,
            isCustom = true
        )
        dao.updateSong(
            song.copy(
                title = newTitle.trim().ifBlank { song.title },
                singerName = newSingerName.trim().ifBlank { song.singerName },
                albumName = newAlbumName.trim().ifBlank { song.albumName },
                singerId = singerId,
                albumId = albumId
            )
        )
        pruneEmptyAlbumsAndSingers()
    }

    suspend fun assignSongToAlbum(songId: Long, album: AlbumFolderEntity) {
        dao.assignSongToAlbum(
            songId = songId,
            albumId = album.id,
            albumName = album.title,
            colorHex = album.coverColorHex
        )
        pruneEmptyAlbumsAndSingers()
    }

    suspend fun assignSongToSinger(songId: Long, singer: SingerFolderEntity) {
        dao.assignSongToSinger(
            songId = songId,
            singerId = singer.id,
            singerName = singer.name
        )
        pruneEmptyAlbumsAndSingers()
    }

    suspend fun deleteSong(song: SongEntity) {
        dao.deleteSong(song)
        pruneEmptyAlbumsAndSingers()
    }

    // Custom Album Folder CRUD
    suspend fun createCustomAlbum(
        title: String,
        singerName: String,
        releaseYear: Int,
        description: String,
        coverColorHex: Long,
        initialSongIds: List<Long>
    ): Long {
        val albumId = dao.insertAlbum(
            AlbumFolderEntity(
                title = title.trim(),
                singerName = singerName.trim().ifBlank { "Various Singers" },
                releaseYear = releaseYear,
                coverColorHex = coverColorHex,
                description = description.trim(),
                isCustom = true
            )
        )
        for (songId in initialSongIds) {
            dao.assignSongToAlbum(
                songId = songId,
                albumId = albumId,
                albumName = title.trim(),
                colorHex = coverColorHex
            )
        }
        return albumId
    }

    suspend fun updateAlbumFolder(
        album: AlbumFolderEntity,
        newTitle: String,
        newSingerName: String,
        newYear: Int,
        newDescription: String,
        newColorHex: Long
    ) {
        val updated = album.copy(
            title = newTitle.trim().ifBlank { album.title },
            singerName = newSingerName.trim().ifBlank { album.singerName },
            releaseYear = newYear,
            description = newDescription.trim(),
            coverColorHex = newColorHex
        )
        dao.updateAlbum(updated)
        dao.syncSongsWithAlbum(updated.id, updated.title, updated.coverColorHex)
    }

    suspend fun deleteAlbumFolder(album: AlbumFolderEntity) {
        dao.detachSongsFromAlbum(album.id)
        dao.deleteAlbum(album)
    }

    // Custom Singer Folder CRUD
    suspend fun createCustomSinger(
        name: String,
        genreTag: String,
        bio: String,
        avatarColorHex: Long,
        initialSongIds: List<Long>
    ): Long {
        val singerId = dao.insertSinger(
            SingerFolderEntity(
                name = name.trim(),
                genreTag = genreTag.trim().ifBlank { "Independent Artist" },
                bio = bio.trim(),
                avatarColorHex = avatarColorHex,
                isCustom = true
            )
        )
        for (songId in initialSongIds) {
            dao.assignSongToSinger(
                songId = songId,
                singerId = singerId,
                singerName = name.trim()
            )
        }
        return singerId
    }

    suspend fun updateSingerFolder(
        singer: SingerFolderEntity,
        newName: String,
        newGenreTag: String,
        newBio: String,
        newColorHex: Long
    ) {
        val updated = singer.copy(
            name = newName.trim().ifBlank { singer.name },
            genreTag = newGenreTag.trim().ifBlank { singer.genreTag },
            bio = newBio.trim(),
            avatarColorHex = newColorHex
        )
        dao.updateSinger(updated)
        dao.syncSongsWithSinger(updated.id, updated.name)
    }

    suspend fun deleteSingerFolder(singer: SingerFolderEntity) {
        dao.detachSongsFromSinger(singer.id)
        dao.deleteSinger(singer)
    }
}
