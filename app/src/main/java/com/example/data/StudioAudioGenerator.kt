package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

data class GeneratedTrackSpec(
    val fileName: String,
    val title: String,
    val singerName: String,
    val singerGenre: String,
    val singerBio: String,
    val singerColorHex: Long,
    val albumTitle: String,
    val albumYear: Int,
    val albumDescription: String,
    val albumColorHex: Long,
    val durationSeconds: Int,
    val chordFrequencies: List<Double>,
    val tempoBpm: Int,
    val isLikedByDefault: Boolean,
    val trackNumber: Int
)

data class GeneratedRecordingSpec(
    val fileName: String,
    val title: String,
    val sourceLabel: String,
    val folderCategory: RecordingCategory,
    val durationSeconds: Int,
    val chordFrequencies: List<Double>,
    val tempoBpm: Int
)

object StudioAudioGenerator {

    val defaultRecordingSpecs = listOf(
        GeneratedRecordingSpec(
            fileName = "call_rec_studio_booking.wav",
            title = "Call Recording • Studio A Booking (+1 415-890-4312)",
            sourceLabel = "Phone Call Recorder",
            folderCategory = RecordingCategory.CALL_RECORDINGS,
            durationSeconds = 11,
            chordFrequencies = listOf(340.0, 440.0, 520.0),
            tempoBpm = 76
        ),
        GeneratedRecordingSpec(
            fileName = "whatsapp_aud_20260318_wa0004.wav",
            title = "WhatsApp Call • Mastering Feedback (Sora)",
            sourceLabel = "WhatsApp Audio",
            folderCategory = RecordingCategory.WHATSAPP_RECORDINGS,
            durationSeconds = 9,
            chordFrequencies = listOf(293.66, 392.00, 493.88),
            tempoBpm = 82
        ),
        GeneratedRecordingSpec(
            fileName = "voice_memo_acoustic_melody_idea.wav",
            title = "Voice Memo #14 • Acoustic Hook Idea",
            sourceLabel = "Voice Recorder",
            folderCategory = RecordingCategory.VOICE_RECORDINGS,
            durationSeconds = 12,
            chordFrequencies = listOf(261.63, 329.63, 392.00),
            tempoBpm = 90
        )
    )

    val defaultStudioSpecs = listOf(
        GeneratedTrackSpec(
            fileName = "midnight_solstice.wav",
            title = "Midnight Solstice",
            singerName = "Klara Voss",
            singerGenre = "Ambient Electronic",
            singerBio = "Berlin-based composer blending warm analog synth pads with minimalist chamber acoustics.",
            singerColorHex = 0xFFFA243CL,
            albumTitle = "Velvet Horizon",
            albumYear = 2026,
            albumDescription = "Late-night analog textures and deep atmospheric progressions.",
            albumColorHex = 0xFFFA243CL,
            durationSeconds = 18,
            chordFrequencies = listOf(220.0, 261.63, 329.63, 392.00), // Am7
            tempoBpm = 96,
            isLikedByDefault = true,
            trackNumber = 1
        ),
        GeneratedTrackSpec(
            fileName = "crimson_echoes.wav",
            title = "Crimson Echoes",
            singerName = "Klara Voss",
            singerGenre = "Ambient Electronic",
            singerBio = "Berlin-based composer blending warm analog synth pads with minimalist chamber acoustics.",
            singerColorHex = 0xFFFA243CL,
            albumTitle = "Velvet Horizon",
            albumYear = 2026,
            albumDescription = "Late-night analog textures and deep atmospheric progressions.",
            albumColorHex = 0xFFFA243CL,
            durationSeconds = 16,
            chordFrequencies = listOf(174.61, 220.0, 261.63, 349.23), // Fmaj7
            tempoBpm = 104,
            isLikedByDefault = false,
            trackNumber = 2
        ),
        GeneratedTrackSpec(
            fileName = "neon_rain_tokyo.wav",
            title = "Neon Rain in Shibuya",
            singerName = "Sora Takahashi",
            singerGenre = "Lo-Fi Jazz & Chillhop",
            singerBio = "Tokyo multi-instrumentalist crafting warm electric piano grooves and downtempo beats.",
            singerColorHex = 0xFF5E5CE6L,
            albumTitle = "Afterhours Tape Vol. 4",
            albumYear = 2025,
            albumDescription = "Intimate Rhodes chords and rain-soaked nocturnal instrumentals.",
            albumColorHex = 0xFF5E5CE6L,
            durationSeconds = 20,
            chordFrequencies = listOf(293.66, 349.23, 440.0, 523.25), // Dm7
            tempoBpm = 84,
            isLikedByDefault = true,
            trackNumber = 1
        ),
        GeneratedTrackSpec(
            fileName = "paper_lantern_drift.wav",
            title = "Paper Lantern Drift",
            singerName = "Sora Takahashi",
            singerGenre = "Lo-Fi Jazz & Chillhop",
            singerBio = "Tokyo multi-instrumentalist crafting warm electric piano grooves and downtempo beats.",
            singerColorHex = 0xFF5E5CE6L,
            albumTitle = "Afterhours Tape Vol. 4",
            albumYear = 2025,
            albumDescription = "Intimate Rhodes chords and rain-soaked nocturnal instrumentals.",
            albumColorHex = 0xFF5E5CE6L,
            durationSeconds = 15,
            chordFrequencies = listOf(196.00, 246.94, 293.66, 392.00), // G6
            tempoBpm = 88,
            isLikedByDefault = false,
            trackNumber = 2
        ),
        GeneratedTrackSpec(
            fileName = "golden_hour_coastline.wav",
            title = "Golden Hour Coastline",
            singerName = "Mateo Silva",
            singerGenre = "Indie Acoustic & Folk",
            singerBio = "Lisbon guitarist and vocalist known for sun-drenched fingerstyle melodies.",
            singerColorHex = 0xFF1DB954L,
            albumTitle = "Atlantic Breeze",
            albumYear = 2026,
            albumDescription = "Warm acoustic resonance recorded live by the coast.",
            albumColorHex = 0xFF1DB954L,
            durationSeconds = 18,
            chordFrequencies = listOf(261.63, 329.63, 392.00, 493.88), // Cmaj7
            tempoBpm = 110,
            isLikedByDefault = true,
            trackNumber = 1
        ),
        GeneratedTrackSpec(
            fileName = "obsidian_pulse.wav",
            title = "Obsidian Pulse",
            singerName = "Nova & The Architects",
            singerGenre = "Minimal Synthwave",
            singerBio = "Stockholm duo designing precision basslines and crystalline arpeggios.",
            singerColorHex = 0xFFFF9F0AL,
            albumTitle = "Monolith Studio Sessions",
            albumYear = 2026,
            albumDescription = "High-contrast analog arpeggios engineered for audiophile headphones.",
            albumColorHex = 0xFFFF9F0AL,
            durationSeconds = 17,
            chordFrequencies = listOf(164.81, 196.00, 246.94, 329.63), // Em
            tempoBpm = 118,
            isLikedByDefault = false,
            trackNumber = 1
        )
    )

    suspend fun ensureWavFile(context: Context, spec: GeneratedTrackSpec): Pair<String, Long> =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "studio_samples")
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, spec.fileName)
            val durationMs = spec.durationSeconds * 1000L

            if (!outFile.exists() || outFile.length() < 1024L) {
                writeSynthesizedWav(
                    file = outFile,
                    durationSeconds = spec.durationSeconds,
                    frequencies = spec.chordFrequencies,
                    tempoBpm = spec.tempoBpm
                )
            }
            Uri.fromFile(outFile).toString() to durationMs
        }

    suspend fun ensureRecordingWavFile(context: Context, spec: GeneratedRecordingSpec): Pair<String, Long> =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "recording_samples")
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, spec.fileName)
            val durationMs = spec.durationSeconds * 1000L

            if (!outFile.exists() || outFile.length() < 1024L) {
                writeSynthesizedWav(
                    file = outFile,
                    durationSeconds = spec.durationSeconds,
                    frequencies = spec.chordFrequencies,
                    tempoBpm = spec.tempoBpm
                )
            }
            Uri.fromFile(outFile).toString() to durationMs
        }

    fun ensureLocalSampleFileExists(context: Context, targetFile: File, durationMs: Long): File {
        if (targetFile.exists() && targetFile.length() > 1024L) return targetFile
        runCatching {
            targetFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
            val matchedStudio = defaultStudioSpecs.find { it.fileName.equals(targetFile.name, ignoreCase = true) }
            val matchedRec = defaultRecordingSpecs.find { it.fileName.equals(targetFile.name, ignoreCase = true) }
            when {
                matchedStudio != null -> writeSynthesizedWav(
                    file = targetFile,
                    durationSeconds = matchedStudio.durationSeconds,
                    frequencies = matchedStudio.chordFrequencies,
                    tempoBpm = matchedStudio.tempoBpm
                )
                matchedRec != null -> writeSynthesizedWav(
                    file = targetFile,
                    durationSeconds = matchedRec.durationSeconds,
                    frequencies = matchedRec.chordFrequencies,
                    tempoBpm = matchedRec.tempoBpm
                )
                else -> writeSynthesizedWav(
                    file = targetFile,
                    durationSeconds = (durationMs / 1000L).toInt().coerceIn(8, 25),
                    frequencies = listOf(220.0, 277.18, 329.63, 440.0),
                    tempoBpm = 96
                )
            }
        }
        return targetFile
    }

    private fun writeSynthesizedWav(
        file: File,
        durationSeconds: Int,
        frequencies: List<Double>,
        tempoBpm: Int
    ) {
        val sampleRate = 44100
        val totalSamples = sampleRate * durationSeconds
        val numChannels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val dataSize = totalSamples * numChannels * bitsPerSample / 8

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        // RIFF header
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(36 + dataSize)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        // fmt subchunk
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16) // PCM chunk size
        header.putShort(1) // AudioFormat = 1 (PCM)
        header.putShort(numChannels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort((numChannels * bitsPerSample / 8).toShort())
        header.putShort(bitsPerSample.toShort())
        // data subchunk
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(dataSize)

        val beatDurationSec = 60.0 / tempoBpm.coerceAtLeast(60)
        FileOutputStream(file).use { fos ->
            fos.write(header.array())
            val buffer = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                val beatIndex = (t / beatDurationSec).toInt()
                val beatPhase = (t % beatDurationSec) / beatDurationSec

                // Arpeggiated note + warm pad chord + sub bass pulse
                val arpFreq = frequencies[beatIndex % frequencies.size] * (if (beatIndex % 8 == 7) 1.5 else 1.0)
                val arpEnv = exp(-3.2 * beatPhase)
                val arpWave = sin(2.0 * PI * arpFreq * t) * 0.35 * arpEnv

                var padWave = 0.0
                for (f in frequencies) {
                    padWave += sin(2.0 * PI * f * 0.5 * t)
                }
                padWave = (padWave / frequencies.size) * 0.32

                val lfo = 0.85 + 0.15 * sin(2.0 * PI * 0.25 * t)
                // Smooth fade in and fade out
                val fadeEnv = when {
                    t < 0.4 -> t / 0.4
                    t > durationSeconds - 0.8 -> ((durationSeconds - t) / 0.8).coerceAtLeast(0.0)
                    else -> 1.0
                }

                val combined = ((arpWave + padWave) * lfo * fadeEnv).coerceIn(-0.95, 0.95)
                val pcmValue = (combined * Short.MAX_VALUE).toInt().toShort()
                buffer.putShort(pcmValue)
                if (!buffer.hasRemaining()) {
                    fos.write(buffer.array())
                    buffer.clear()
                }
            }
            if (buffer.position() > 0) {
                fos.write(buffer.array(), 0, buffer.position())
            }
        }
    }
}
