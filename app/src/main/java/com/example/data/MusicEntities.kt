package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "album_folders")
data class AlbumFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val singerName: String,
    val releaseYear: Int = 2026,
    val coverColorHex: Long = 0xFFFA243CL,
    val artworkPath: String? = null,
    val description: String = "",
    val isCustom: Boolean = true,
    val playCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "singer_folders")
data class SingerFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val genreTag: String = "Alternative & Electronic",
    val bio: String = "",
    val avatarColorHex: Long = 0xFF5E5CE6L,
    val artworkPath: String? = null,
    val isCustom: Boolean = true,
    val playCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class RecordingCategory(
    val id: String,
    val folderTitle: String,
    val subtitle: String,
    val badgeLabel: String,
    val colorHex: Long
) {
    CALL_RECORDINGS(
        id = "CALL_RECORDINGS",
        folderTitle = "Call Recordings",
        subtitle = "Cellular & phone call audio logs",
        badgeLabel = "Call Audio",
        colorHex = 0xFFFF9F0AL
    ),
    WHATSAPP_RECORDINGS(
        id = "WHATSAPP_RECORDINGS",
        folderTitle = "WhatsApp Recordings",
        subtitle = "WhatsApp call recordings & voice notes",
        badgeLabel = "WhatsApp",
        colorHex = 0xFF1DB954L
    ),
    VOICE_RECORDINGS(
        id = "VOICE_RECORDINGS",
        folderTitle = "Voice Recordings & Memos",
        subtitle = "Voice memos, dictations & sound recorder clips",
        badgeLabel = "Voice Memo",
        colorHex = 0xFF5E5CE6L
    );

    companion object {
        fun fromId(id: String?): RecordingCategory? {
            if (id.isNullOrBlank()) return null
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
        }
    }
}

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val singerName: String,
    val albumName: String,
    val albumId: Long? = null,
    val singerId: Long? = null,
    val uriString: String,
    val durationMs: Long,
    val isLiked: Boolean = false,
    val colorHex: Long = 0xFFFA243CL,
    val artworkPath: String? = null,
    val trackNumber: Int = 1,
    val playCount: Int = 0,
    val sourceType: String = "DEVICE", // "DEVICE", "IMPORTED", "STUDIO_SAMPLE", "ZIP_ARCHIVE"
    val recordingCategory: String? = null, // null = Music, or RecordingCategory.id
    val dateAdded: Long = System.currentTimeMillis()
)
