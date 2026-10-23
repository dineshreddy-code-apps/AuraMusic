package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    // Music Songs (strictly excludes Call, WhatsApp, and Voice recordings)
    @Query("SELECT * FROM songs WHERE recordingCategory IS NULL ORDER BY dateAdded DESC, title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isLiked = 1 AND recordingCategory IS NULL ORDER BY dateAdded DESC")
    fun getLikedSongs(): Flow<List<SongEntity>>

    // Recordings (Call Recordings, WhatsApp Recordings, Voice Recordings/Memos)
    @Query("SELECT * FROM songs WHERE recordingCategory IS NOT NULL ORDER BY dateAdded DESC, title ASC")
    fun getAllRecordings(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs")
    suspend fun getAllSongsSnapshot(): List<SongEntity>

    @Query("SELECT COUNT(*) FROM songs WHERE recordingCategory IS NULL")
    suspend fun getSongCount(): Int

    @Query("SELECT COUNT(*) FROM songs WHERE recordingCategory IS NOT NULL")
    suspend fun getRecordingCount(): Int

    @Query("SELECT * FROM songs WHERE uriString = :uri LIMIT 1")
    suspend fun getSongByUri(uri: String): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Delete
    suspend fun deleteSong(song: SongEntity)

    @Query("UPDATE songs SET isLiked = :isLiked WHERE id = :songId")
    suspend fun setSongLiked(songId: Long, isLiked: Boolean)

    @Query("UPDATE songs SET recordingCategory = :category, albumId = :albumId, singerId = :singerId, albumName = :albumName, colorHex = :colorHex WHERE id = :songId")
    suspend fun updateSongRecordingCategory(
        songId: Long,
        category: String?,
        albumId: Long?,
        singerId: Long?,
        albumName: String,
        colorHex: Long
    )

    @Query("UPDATE songs SET playCount = playCount + 1 WHERE id = :songId")
    suspend fun incrementSongPlayCount(songId: Long)

    @Query("UPDATE album_folders SET playCount = playCount + 1 WHERE id = :albumId")
    suspend fun incrementAlbumPlayCount(albumId: Long)

    @Query("UPDATE singer_folders SET playCount = playCount + 1 WHERE id = :singerId")
    suspend fun incrementSingerPlayCount(singerId: Long)

    @Query("UPDATE songs SET albumId = :albumId, albumName = :albumName, colorHex = :colorHex, recordingCategory = NULL WHERE id = :songId")
    suspend fun assignSongToAlbum(songId: Long, albumId: Long?, albumName: String, colorHex: Long)

    @Query("UPDATE songs SET singerId = :singerId, singerName = :singerName, recordingCategory = NULL WHERE id = :songId")
    suspend fun assignSongToSinger(songId: Long, singerId: Long?, singerName: String)

    // Album Folders
    @Query("SELECT * FROM album_folders ORDER BY createdAt DESC, title ASC")
    fun getAllAlbums(): Flow<List<AlbumFolderEntity>>

    @Query("SELECT * FROM album_folders")
    suspend fun getAllAlbumsSnapshot(): List<AlbumFolderEntity>

    @Query("SELECT * FROM album_folders WHERE LOWER(title) = LOWER(:title) LIMIT 1")
    suspend fun getAlbumByTitle(title: String): AlbumFolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumFolderEntity): Long

    @Update
    suspend fun updateAlbum(album: AlbumFolderEntity)

    @Delete
    suspend fun deleteAlbum(album: AlbumFolderEntity)

    @Query("UPDATE songs SET albumId = NULL, albumName = 'Single' WHERE albumId = :albumId")
    suspend fun detachSongsFromAlbum(albumId: Long)

    @Query("UPDATE songs SET albumName = :newTitle, colorHex = :colorHex WHERE albumId = :albumId")
    suspend fun syncSongsWithAlbum(albumId: Long, newTitle: String, colorHex: Long)

    @Query("UPDATE album_folders SET artworkPath = :artworkPath, coverColorHex = :colorHex WHERE id = :albumId AND (artworkPath IS NULL OR artworkPath = '')")
    suspend fun updateAlbumArtworkIfBlank(albumId: Long, artworkPath: String, colorHex: Long)

    // Singer Folders
    @Query("SELECT * FROM singer_folders ORDER BY createdAt DESC, name ASC")
    fun getAllSingers(): Flow<List<SingerFolderEntity>>

    @Query("SELECT * FROM singer_folders")
    suspend fun getAllSingersSnapshot(): List<SingerFolderEntity>

    @Query("SELECT * FROM singer_folders WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getSingerByName(name: String): SingerFolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSinger(singer: SingerFolderEntity): Long

    @Update
    suspend fun updateSinger(singer: SingerFolderEntity)

    @Delete
    suspend fun deleteSinger(singer: SingerFolderEntity)

    @Query("UPDATE songs SET singerId = NULL WHERE singerId = :singerId")
    suspend fun detachSongsFromSinger(singerId: Long)

    @Query("UPDATE songs SET singerName = :newName WHERE singerId = :singerId")
    suspend fun syncSongsWithSinger(singerId: Long, newName: String)

    @Query("UPDATE singer_folders SET artworkPath = :artworkPath WHERE id = :singerId AND (artworkPath IS NULL OR artworkPath = '')")
    suspend fun updateSingerArtworkIfBlank(singerId: Long, artworkPath: String)
}
