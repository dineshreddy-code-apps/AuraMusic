package com.example.data

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.ui.theme.StudioCoverPalettes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlin.math.abs

data class ScannedAudioItem(
    val title: String,
    val singerName: String,
    val albumName: String,
    val uriString: String,
    val durationMs: Long,
    val trackNumber: Int,
    val colorHex: Long,
    val artworkPath: String? = null,
    val sourceType: String,
    val recordingCategory: String? = null
)

object DeviceAudioScanner {

    private val SUPPORTED_AUDIO_EXTENSIONS = setOf(
        "mp3", "wav", "flac", "m4a", "aac", "ogg", "opus", "wma", "mp4", "amr", "3gp"
    )

    private val SUPPORTED_IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp"
    )

    /**
     * Classifies whether an audio file is a Call Recording, WhatsApp Call/Voice Recording,
     * Voice Recording / Voice Memo, or standard Music (`null`).
     */
    fun detectRecordingCategory(
        title: String,
        artist: String,
        album: String,
        pathOrFileName: String = "",
        isMusicFlag: Int = 1
    ): RecordingCategory? {
        val combined = "$title | $artist | $album | $pathOrFileName".lowercase(Locale.US)

        // 1. WhatsApp Call Recordings & WhatsApp Voice/Audio Notes
        val isWhatsApp = combined.contains("whatsapp") ||
            combined.contains("wa_call") ||
            combined.contains("wacall") ||
            combined.contains("ptt-20") ||
            combined.contains("aud-20") ||
            Regex("\\bptt[-_]\\d+").containsMatchIn(combined)

        if (isWhatsApp) {
            return RecordingCategory.WHATSAPP_RECORDINGS
        }

        // 2. Phone / Cellular Call Recordings
        val isCallRecording = combined.contains("call_rec") ||
            combined.contains("callrec") ||
            combined.contains("call recording") ||
            combined.contains("call_recording") ||
            combined.contains("callrecordings") ||
            combined.contains("phone call") ||
            combined.contains("record/call") ||
            combined.contains("recordings/call") ||
            combined.contains("sound_recorder/call") ||
            combined.contains("incall") ||
            combined.contains("cube call") ||
            combined.contains("acr_") ||
            Regex("\\bcall[-_ ]?\\d{4}").containsMatchIn(combined)

        if (isCallRecording) {
            return RecordingCategory.CALL_RECORDINGS
        }

        // 3. Voice Recordings / Voice Memos / Sound Recorder clips
        val isVoiceMemo = isMusicFlag == 0 ||
            combined.contains("voice") ||
            combined.contains("memo") ||
            combined.contains("recorder") ||
            combined.contains("recording") ||
            combined.contains("sound_recorder") ||
            combined.contains("sound recorder") ||
            combined.contains("dictaphone") ||
            combined.contains("audio_rec") ||
            combined.contains("vn-") ||
            combined.contains("voicenote")

        if (isVoiceMemo) {
            return RecordingCategory.VOICE_RECORDINGS
        }

        return null
    }

    suspend fun scanDeviceMediaStore(context: Context): List<ScannedAudioItem> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<ScannedAudioItem>()
            val projectionList = mutableListOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media.DISPLAY_NAME
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                projectionList.add(MediaStore.Audio.Media.RELATIVE_PATH)
            }

            val selection = "${MediaStore.Audio.Media.DURATION} >= 1000"
            val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

            runCatching {
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projectionList.toTypedArray(),
                    selection,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                    val isMusicCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC)
                    val displayNameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                    val relPathCol = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                    } else {
                        -1
                    }

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val rawTitle = cursor.getString(titleCol)?.trim().orEmpty()
                        val rawArtist = cursor.getString(artistCol)?.trim().orEmpty()
                        val rawAlbum = cursor.getString(albumCol)?.trim().orEmpty()
                        val duration = cursor.getLong(durationCol).coerceAtLeast(1000L)
                        val trackNum = cursor.getInt(trackCol).let { if (it > 0) it % 1000 else 1 }
                        val isMusicFlag = if (isMusicCol != -1) cursor.getInt(isMusicCol) else 1
                        val displayName = if (displayNameCol != -1) cursor.getString(displayNameCol).orEmpty() else ""
                        val relativePath = if (relPathCol != -1) cursor.getString(relPathCol).orEmpty() else ""

                        val cleanTitle = if (rawTitle.isBlank() || rawTitle == "<unknown>") {
                            displayName.substringBeforeLast(".").ifBlank { "Track #$id" }
                        } else {
                            rawTitle
                        }
                        val cleanArtist = if (rawArtist.isBlank() || rawArtist == "<unknown>") {
                            "Unknown Singer"
                        } else {
                            rawArtist
                        }
                        val cleanAlbum = if (rawAlbum.isBlank() || rawAlbum == "<unknown>") {
                            "Local Device Imports"
                        } else {
                            rawAlbum
                        }

                        val detectedRecording = detectRecordingCategory(
                            title = cleanTitle,
                            artist = cleanArtist,
                            album = cleanAlbum,
                            pathOrFileName = "$relativePath/$displayName",
                            isMusicFlag = isMusicFlag
                        )

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        val paletteIndex = abs((cleanAlbum + cleanArtist).hashCode()) % StudioCoverPalettes.size
                        var fallbackColor = detectedRecording?.colorHex ?: StudioCoverPalettes[paletteIndex]
                        var extractedArtworkPath: String? = null

                        // Extract embedded album art for music tracks
                        if (detectedRecording == null) {
                            val retriever = MediaMetadataRetriever()
                            try {
                                retriever.setDataSource(context, contentUri)
                                val artBytes = retriever.embeddedPicture
                                if (artBytes != null && artBytes.isNotEmpty()) {
                                    val (savedPath, sampledColor) = saveEmbeddedArtworkAndSampleColor(
                                        context = context,
                                        artBytes = artBytes,
                                        keyHint = "${cleanAlbum}_${cleanArtist}_$id",
                                        defaultColor = fallbackColor
                                    )
                                    extractedArtworkPath = savedPath
                                    fallbackColor = sampledColor
                                }
                            } catch (_: Exception) {
                                // Ignore if track lacks embedded picture
                            } finally {
                                runCatching { retriever.release() }
                            }
                        }

                        results.add(
                            ScannedAudioItem(
                                title = cleanTitle,
                                singerName = detectedRecording?.badgeLabel ?: cleanArtist,
                                albumName = detectedRecording?.folderTitle ?: cleanAlbum,
                                uriString = contentUri.toString(),
                                durationMs = duration,
                                trackNumber = trackNum,
                                colorHex = fallbackColor,
                                artworkPath = extractedArtworkPath,
                                sourceType = "DEVICE",
                                recordingCategory = detectedRecording?.id
                            )
                        )
                    }
                }
            }
            results
        }

    suspend fun parseImportedUris(
        context: Context,
        uris: List<Uri>,
        forcedRecordingCategory: RecordingCategory? = null
    ): List<ScannedAudioItem> =
        withContext(Dispatchers.IO) {
            val importedItems = mutableListOf<ScannedAudioItem>()
            for (uri in uris) {
                runCatching {
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }

                    val displayName = queryDisplayName(context, uri)
                    val mimeType = context.contentResolver.getType(uri)?.lowercase(Locale.US).orEmpty()

                    val isZipFile = displayName.lowercase(Locale.US).endsWith(".zip") ||
                        mimeType.contains("zip") ||
                        hasZipMagicHeader(context, uri)

                    if (isZipFile) {
                        val extractedFromZip = extractAudioFromZipUri(context, uri, displayName, forcedRecordingCategory)
                        importedItems.addAll(extractedFromZip)
                    } else {
                        val singleAudio = parseSingleAudioUri(context, uri, displayName, forcedRecordingCategory)
                        importedItems.add(singleAudio)
                    }
                }
            }
            importedItems
        }

    suspend fun extractAudioFromZipUris(context: Context, uris: List<Uri>): List<ScannedAudioItem> =
        withContext(Dispatchers.IO) {
            val extractedItems = mutableListOf<ScannedAudioItem>()
            for (uri in uris) {
                runCatching {
                    val displayName = queryDisplayName(context, uri)
                    extractedItems.addAll(extractAudioFromZipUri(context, uri, displayName, null))
                }
            }
            extractedItems
        }

    private fun queryDisplayName(context: Context, uri: Uri): String {
        var fallbackDisplayName = uri.lastPathSegment ?: "Imported_Audio"
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIdx != -1 && cursor.moveToFirst()) {
                    fallbackDisplayName = cursor.getString(nameIdx) ?: fallbackDisplayName
                }
            }
        }
        return fallbackDisplayName
    }

    private fun hasZipMagicHeader(context: Context, uri: Uri): Boolean {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(4)
                val read = stream.read(header)
                read == 4 &&
                    header[0] == 0x50.toByte() && // 'P'
                    header[1] == 0x4B.toByte() && // 'K'
                    header[2] == 0x03.toByte() &&
                    header[3] == 0x04.toByte()
            } ?: false
        }.getOrDefault(false)
    }

    private fun extractAudioFromZipUri(
        context: Context,
        zipUri: Uri,
        zipDisplayName: String,
        forcedRecordingCategory: RecordingCategory? = null
    ): List<ScannedAudioItem> {
        val extractedTracks = mutableListOf<ScannedAudioItem>()
        var archiveCoverImagePath: String? = null

        val zipBaseName = zipDisplayName
            .substringBeforeLast(".")
            .replace("_", " ")
            .trim()
            .ifBlank { "ZIP Archive Album" }

        val safeArchiveFolder = zipBaseName.replace(Regex("[^a-zA-Z0-9 _-]"), "").trim().ifBlank { "zip_album" }
        val rootExtractDir = File(File(context.filesDir, "zip_imports"), safeArchiveFolder).apply {
            if (!exists()) mkdirs()
        }
        val canonicalRootPath = rootExtractDir.canonicalPath

        context.contentResolver.openInputStream(zipUri)?.use { rawStream ->
            ZipInputStream(BufferedInputStream(rawStream)).use { zis ->
                var entry = zis.nextEntry
                var orderCounter = 1
                while (entry != null) {
                    val entryName = entry.name
                    if (!entry.isDirectory && !shouldIgnoreZipEntry(entryName)) {
                        val ext = entryName.substringAfterLast('.', "").lowercase(Locale.US)
                        val segments = entryName.split("/", "\\").filter { it.isNotBlank() }
                        val rawFileName = segments.lastOrNull() ?: "item_$orderCounter.$ext"

                        if (ext in SUPPORTED_AUDIO_EXTENSIONS) {
                            val subfolderName = if (segments.size >= 2) {
                                segments[segments.size - 2].replace("_", " ").trim()
                            } else {
                                zipBaseName
                            }

                            val safeFileName = "${orderCounter}_${rawFileName.replace(Regex("[^a-zA-Z0-9._ -]"), "_")}"
                            val outFile = File(rootExtractDir, safeFileName)

                            if (outFile.canonicalPath.startsWith(canonicalRootPath)) {
                                FileOutputStream(outFile).use { fos ->
                                    val buffer = ByteArray(8192)
                                    var len: Int
                                    while (zis.read(buffer).also { len = it } > 0) {
                                        fos.write(buffer, 0, len)
                                    }
                                }

                                if (outFile.exists() && outFile.length() > 128L) {
                                    val item = parseExtractedAudioFile(
                                        context = context,
                                        file = outFile,
                                        rawFileName = rawFileName,
                                        defaultAlbumName = subfolderName.ifBlank { zipBaseName },
                                        fallbackTrackNumber = orderCounter,
                                        forcedRecordingCategory = forcedRecordingCategory
                                    )
                                    extractedTracks.add(item)
                                    orderCounter++
                                }
                            }
                        } else if (ext in SUPPORTED_IMAGE_EXTENSIONS && archiveCoverImagePath == null) {
                            // Also extract standalone album cover image if bundled in the ZIP archive
                            val artDir = File(context.filesDir, "extracted_artwork").apply {
                                if (!exists()) mkdirs()
                            }
                            val coverFile = File(artDir, "zip_cover_${abs(safeArchiveFolder.hashCode())}.$ext")
                            FileOutputStream(coverFile).use { fos ->
                                val buffer = ByteArray(8192)
                                var len: Int
                                while (zis.read(buffer).also { len = it } > 0) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                            if (coverFile.exists() && coverFile.length() > 256L) {
                                archiveCoverImagePath = coverFile.absolutePath
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        // If any track in the ZIP didn't have embedded ID3 cover art, use the cover image found in the ZIP
        return if (archiveCoverImagePath != null) {
            extractedTracks.map { item ->
                if (item.artworkPath == null && item.recordingCategory == null) {
                    item.copy(artworkPath = archiveCoverImagePath)
                } else {
                    item
                }
            }
        } else {
            extractedTracks
        }
    }

    private fun shouldIgnoreZipEntry(entryName: String): Boolean {
        val normalized = entryName.replace("\\", "/")
        val fileName = normalized.substringAfterLast("/")
        return normalized.startsWith("__MACOSX/") ||
            fileName.startsWith("._") ||
            fileName.startsWith(".")
    }

    private fun parseExtractedAudioFile(
        context: Context,
        file: File,
        rawFileName: String,
        defaultAlbumName: String,
        fallbackTrackNumber: Int,
        forcedRecordingCategory: RecordingCategory? = null
    ): ScannedAudioItem {
        val baseTitle = rawFileName
            .substringBeforeLast(".")
            .replace("_", " ")
            .trim()

        val inferredSingerFromName = if (baseTitle.contains(" - ")) {
            baseTitle.substringBefore(" - ").trim().ifBlank { "Imported Singer" }
        } else {
            "Imported Singer"
        }
        val inferredTitleFromName = if (baseTitle.contains(" - ")) {
            baseTitle.substringAfter(" - ").trim().ifBlank { baseTitle }
        } else {
            baseTitle.ifBlank { "Track $fallbackTrackNumber" }
        }

        var title = inferredTitleFromName
        var artist = inferredSingerFromName
        var album = defaultAlbumName
        var durationMs = 180_000L
        var trackNum = fallbackTrackNumber
        var extractedArtworkPath: String? = null

        val paletteIndex = abs((album + artist + title).hashCode()) % StudioCoverPalettes.size
        var colorHex = StudioCoverPalettes[paletteIndex]

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
                ?.let { title = it.trim() }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }
                ?.let { artist = it.trim() }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }
                ?.let { album = it.trim() }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
                ?.let { durationMs = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.substringBefore("/")
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
                ?.let { trackNum = it }

            val artBytes = retriever.embeddedPicture
            if (artBytes != null && artBytes.isNotEmpty()) {
                val (savedPath, sampledColor) = saveEmbeddedArtworkAndSampleColor(
                    context = context,
                    artBytes = artBytes,
                    keyHint = "${album}_${artist}_$title",
                    defaultColor = colorHex
                )
                extractedArtworkPath = savedPath
                colorHex = sampledColor
            }
        } catch (_: Exception) {
            // Keep filename & folder derived metadata
        } finally {
            runCatching { retriever.release() }
        }

        val resolvedRecording = forcedRecordingCategory ?: detectRecordingCategory(
            title = title,
            artist = artist,
            album = album,
            pathOrFileName = rawFileName,
            isMusicFlag = 1
        )

        return ScannedAudioItem(
            title = title,
            singerName = resolvedRecording?.badgeLabel ?: artist,
            albumName = resolvedRecording?.folderTitle ?: album,
            uriString = Uri.fromFile(file).toString(),
            durationMs = durationMs,
            trackNumber = trackNum,
            colorHex = resolvedRecording?.colorHex ?: colorHex,
            artworkPath = if (resolvedRecording == null) extractedArtworkPath else null,
            sourceType = "ZIP_ARCHIVE",
            recordingCategory = resolvedRecording?.id
        )
    }

    private fun parseSingleAudioUri(
        context: Context,
        uri: Uri,
        fallbackDisplayName: String,
        forcedRecordingCategory: RecordingCategory? = null
    ): ScannedAudioItem {
        val strippedName = fallbackDisplayName
            .substringBeforeLast(".")
            .replace("_", " ")
            .trim()
            .ifBlank { "Imported Audio" }

        val retriever = MediaMetadataRetriever()
        var title = strippedName
        var artist = "Imported Singer"
        var album = "Imported Collection"
        var durationMs = 180_000L
        var trackNum = 1
        var extractedArtworkPath: String? = null

        val paletteIndex = abs((album + artist + title).hashCode()) % StudioCoverPalettes.size
        var colorHex = StudioCoverPalettes[paletteIndex]

        try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
                ?.let { title = it.trim() }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }
                ?.let { artist = it.trim() }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }
                ?.let { album = it.trim() }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
                ?.let { durationMs = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.substringBefore("/")
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
                ?.let { trackNum = it }

            val updatedPaletteIdx = abs((album + artist + title).hashCode()) % StudioCoverPalettes.size
            colorHex = StudioCoverPalettes[updatedPaletteIdx]

            val artBytes = retriever.embeddedPicture
            if (artBytes != null && artBytes.isNotEmpty()) {
                val (savedPath, sampledColor) = saveEmbeddedArtworkAndSampleColor(
                    context = context,
                    artBytes = artBytes,
                    keyHint = "${album}_${artist}_$title",
                    defaultColor = colorHex
                )
                extractedArtworkPath = savedPath
                colorHex = sampledColor
            }
        } catch (_: Exception) {
            // Use filename-derived metadata if MediaMetadataRetriever cannot read tags
        } finally {
            runCatching { retriever.release() }
        }

        val resolvedRecording = forcedRecordingCategory ?: detectRecordingCategory(
            title = title,
            artist = artist,
            album = album,
            pathOrFileName = fallbackDisplayName,
            isMusicFlag = 1
        )

        val resolvedPlayableUriString = runCatching {
            val importsDir = File(context.filesDir, "audio_imports").apply {
                if (!exists()) mkdirs()
            }
            val ext = fallbackDisplayName.substringAfterLast('.', "mp3")
                .lowercase(Locale.US)
                .takeIf { it in SUPPORTED_AUDIO_EXTENSIONS } ?: "mp3"
            val safeName = fallbackDisplayName
                .substringBeforeLast(".")
                .replace(Regex("[^a-zA-Z0-9._ -]"), "_")
                .take(64)
                .ifBlank { "imported_track" }
            val uniqueHash = abs(uri.toString().hashCode())
            val localCopy = File(importsDir, "${safeName}_$uniqueHash.$ext")
            if (!localCopy.exists() || localCopy.length() == 0L) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(localCopy).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            if (localCopy.exists() && localCopy.length() > 128L) {
                Uri.fromFile(localCopy).toString()
            } else {
                uri.toString()
            }
        }.getOrDefault(uri.toString())

        return ScannedAudioItem(
            title = title,
            singerName = resolvedRecording?.badgeLabel ?: artist,
            albumName = resolvedRecording?.folderTitle ?: album,
            uriString = resolvedPlayableUriString,
            durationMs = durationMs,
            trackNumber = trackNum,
            colorHex = resolvedRecording?.colorHex ?: colorHex,
            artworkPath = if (resolvedRecording == null) extractedArtworkPath else null,
            sourceType = "IMPORTED",
            recordingCategory = resolvedRecording?.id
        )
    }

    /**
     * Saves embedded cover artwork bytes to app-private storage and samples a vibrant accent color
     * from the image so dynamic gradients match the extracted album cover.
     */
    private fun saveEmbeddedArtworkAndSampleColor(
        context: Context,
        artBytes: ByteArray,
        keyHint: String,
        defaultColor: Long
    ): Pair<String?, Long> {
        return runCatching {
            val artDir = File(context.filesDir, "extracted_artwork").apply {
                if (!exists()) mkdirs()
            }
            val fileHash = abs(keyHint.hashCode())
            val outFile = File(artDir, "cover_$fileHash.jpg")
            if (!outFile.exists() || outFile.length() == 0L) {
                FileOutputStream(outFile).use { fos ->
                    fos.write(artBytes)
                }
            }

            // Sample dominant/center color from a tiny downsampled bitmap
            val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
            val bmp = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, opts)
            var sampledHex = defaultColor
            if (bmp != null && bmp.width > 0 && bmp.height > 0) {
                val pixel = bmp.getPixel(bmp.width / 2, bmp.height / 2)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                // Ensure the sampled color isn't pure black or pure white for ambient gradients
                if ((r + g + b) in 60..680) {
                    sampledHex = (0xFFL shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
                }
                bmp.recycle()
            }
            outFile.absolutePath to sampledHex
        }.getOrElse { null to defaultColor }
    }
}
