package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import com.example.data.SongEntity
import com.example.data.StudioAudioGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import kotlin.math.abs

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class EqualizerPreset(
    val label: String,
    val subtitle: String,
    val bandGainsDb: List<Float> // 5 bands: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz (-12dB to +12dB)
) {
    FLAT("Flat Studio", "Neutral reference response", listOf(0f, 0f, 0f, 0f, 0f)),
    BASS_BOOST("Deep Bass", "Punchy low-end warmth", listOf(7.5f, 5.0f, 0.0f, 1.0f, 2.0f)),
    ACOUSTIC("Acoustic Warmth", "Natural string & room detail", listOf(3.0f, 2.5f, 1.0f, 3.5f, 4.0f)),
    VOCAL_CLARITY("Vocal Presence", "Forward singer mids & air", listOf(-2.0f, 1.5f, 5.5f, 4.5f, 2.5f)),
    ELECTRONIC("Electronic & Synth", "V-curve sub and crisp highs", listOf(6.5f, 2.0f, -1.5f, 3.5f, 6.5f)),
    LATE_NIGHT("Late Night", "Gentle highs & balanced lows", listOf(2.5f, 1.5f, 0.5f, -2.5f, -4.0f)),
    CUSTOM("Custom Tuning", "Locked user 5-band studio curve", listOf(0f, 0f, 0f, 0f, 0f))
}

data class SavedCustomTuning(
    val id: String,
    val name: String,
    val bandGainsDb: List<Float>, // 5 bands: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz
    val bassBoostStrength: Int    // 0..1000
)

data class PlaybackState(
    val currentSong: SongEntity? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 1L,
    val queue: List<SongEntity> = emptyList(),
    val currentQueueIndex: Int = -1,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val autoContinuationLabel: String? = null,
    val equalizerEnabled: Boolean = true,
    val selectedPreset: EqualizerPreset = EqualizerPreset.FLAT,
    val customBandGainsDb: List<Float> = EqualizerPreset.FLAT.bandGainsDb,
    val bassBoostStrength: Int = 0, // 0..1000
    val savedCustomTunings: List<SavedCustomTuning> = emptyList(),
    val activeCustomTuningId: String? = null,
    val activeCustomTuningName: String? = null,
    val sleepTimerRemainingMs: Long? = null,
    val stopAtEndOfTrack: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val waveformAmplitudes: List<Float> = List(36) { idx ->
        0.22f + 0.18f * kotlin.math.sin(idx * 0.45f)
    }
)

class AudioPlayerController private constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val fxPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val playbackAudioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private var audioFocusRequest: AudioFocusRequest? = null

    private var userCustomGainsDb: List<Float> = loadPersistedCustomGains()
    private var basePrimaryQueue: List<SongEntity> = emptyList()
    private var pendingContinuationQueue: List<SongEntity> = emptyList()
    private var currentContinuationDescription: String? = null

    private val _state = MutableStateFlow(loadInitialPlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var isPlayerPrepared: Boolean = false
    private var shouldPlayWhenPrepared: Boolean = false
    private var pendingSeekPositionMs: Long? = null

    private var equalizer: Equalizer? = null
    private var equalizerSessionId: Int = -1
    private var bassBoost: BassBoost? = null
    private var bassBoostSessionId: Int = -1
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    private fun requestMediaAudioFocus() {
        val am = audioManager ?: return
        runCatching {
            if (am.mode != AudioManager.MODE_NORMAL) {
                am.mode = AudioManager.MODE_NORMAL
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = audioFocusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAudioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setWillPauseWhenDucked(false)
                    .setOnAudioFocusChangeListener { focusChange ->
                        when (focusChange) {
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                                runCatching { mediaPlayer?.setVolume(0.35f, 0.35f) }
                            }
                            AudioManager.AUDIOFOCUS_GAIN -> {
                                runCatching { mediaPlayer?.setVolume(1.0f, 1.0f) }
                            }
                        }
                    }
                    .build()
                    .also { audioFocusRequest = it }
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        }
    }

    private fun loadPersistedCustomGains(): List<Float> {
        val raw = fxPrefs.getString(KEY_CUSTOM_BANDS, null)
        if (!raw.isNullOrBlank()) {
            val parsed = raw.split(",").mapNotNull { it.toFloatOrNull()?.coerceIn(-12f, 12f) }
            if (parsed.size == 5) return parsed
        }
        return EqualizerPreset.FLAT.bandGainsDb
    }

    private fun loadPersistedSavedTunings(): List<SavedCustomTuning> {
        val raw = fxPrefs.getString(KEY_SAVED_CUSTOM_TUNINGS, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size < 4) return@mapNotNull null
            val id = parts[0].trim()
            val name = parts[1].trim()
            val bands = parts[2].split(",").mapNotNull { it.toFloatOrNull()?.coerceIn(-12f, 12f) }
            val bass = parts[3].toIntOrNull()?.coerceIn(0, 1000) ?: 0
            if (id.isNotEmpty() && name.isNotEmpty() && bands.size == 5) {
                SavedCustomTuning(
                    id = id,
                    name = name,
                    bandGainsDb = bands,
                    bassBoostStrength = bass
                )
            } else {
                null
            }
        }
    }

    private fun serializeSavedTunings(tunings: List<SavedCustomTuning>): String {
        return tunings.joinToString("\n") { t ->
            val cleanName = t.name.replace("|", " ").replace("\n", " ").trim()
            val bandsStr = t.bandGainsDb.joinToString(",") { it.toString() }
            "${t.id}|$cleanName|$bandsStr|${t.bassBoostStrength}"
        }
    }

    private fun loadInitialPlaybackState(): PlaybackState {
        val eqEnabled = fxPrefs.getBoolean(KEY_EQ_ENABLED, true)
        val presetName = fxPrefs.getString(KEY_EQ_PRESET, EqualizerPreset.FLAT.name) ?: EqualizerPreset.FLAT.name
        val preset = runCatching { EqualizerPreset.valueOf(presetName) }.getOrDefault(EqualizerPreset.FLAT)
        val savedBands = loadPersistedCustomGains()
        val activeBands = if (preset == EqualizerPreset.CUSTOM) savedBands else preset.bandGainsDb
        val bassStrength = fxPrefs.getInt(KEY_BASS_STRENGTH, 0).coerceIn(0, 1000)
        val savedTunings = loadPersistedSavedTunings()
        val activeTuningId = fxPrefs.getString(KEY_ACTIVE_CUSTOM_TUNING_ID, null)
        val activeTuning = savedTunings.find { it.id == activeTuningId }
        val repeatModeName = fxPrefs.getString(KEY_REPEAT_MODE, RepeatMode.OFF.name) ?: RepeatMode.OFF.name
        val savedRepeatMode = runCatching { RepeatMode.valueOf(repeatModeName) }.getOrDefault(RepeatMode.OFF)

        return PlaybackState(
            repeatMode = savedRepeatMode,
            equalizerEnabled = eqEnabled,
            selectedPreset = preset,
            customBandGainsDb = activeTuning?.bandGainsDb ?: activeBands,
            bassBoostStrength = activeTuning?.bassBoostStrength ?: bassStrength,
            savedCustomTunings = savedTunings,
            activeCustomTuningId = activeTuning?.id,
            activeCustomTuningName = activeTuning?.name
        )
    }

    private fun persistStudioFxState(
        enabled: Boolean = _state.value.equalizerEnabled,
        preset: EqualizerPreset = _state.value.selectedPreset,
        bands: List<Float> = _state.value.customBandGainsDb,
        bassStrength: Int = _state.value.bassBoostStrength,
        savedTunings: List<SavedCustomTuning> = _state.value.savedCustomTunings,
        activeTuningId: String? = _state.value.activeCustomTuningId
    ) {
        fxPrefs.edit()
            .putBoolean(KEY_EQ_ENABLED, enabled)
            .putString(KEY_EQ_PRESET, preset.name)
            .putString(KEY_CUSTOM_BANDS, bands.joinToString(",") { it.toString() })
            .putInt(KEY_BASS_STRENGTH, bassStrength)
            .putString(KEY_SAVED_CUSTOM_TUNINGS, serializeSavedTunings(savedTunings))
            .putString(KEY_ACTIVE_CUSTOM_TUNING_ID, activeTuningId)
            .apply()
    }

    /**
     * Populates the initial song and queue in a paused, ready state without calling
     * start() while MediaPlayer is preparing (which would cause error -38).
     */
    fun prepareInitialSong(song: SongEntity, contextQueue: List<SongEntity> = listOf(song)) {
        if (song.recordingCategory != null) return
        val musicOnlyQueue = contextQueue.filter { it.recordingCategory == null }
        val baseQueue = if (musicOnlyQueue.isEmpty()) listOf(song) else musicOnlyQueue
        basePrimaryQueue = baseQueue
        pendingContinuationQueue = emptyList()
        currentContinuationDescription = null
        val initialQueue = if (_state.value.repeatMode == RepeatMode.ONE) {
            listOf(song)
        } else {
            baseQueue
        }
        val targetIndex = initialQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        _state.update {
            it.copy(
                currentSong = song,
                queue = initialQueue,
                currentQueueIndex = targetIndex,
                autoContinuationLabel = null,
                isPlaying = false,
                currentPositionMs = 0L,
                durationMs = song.durationMs.coerceAtLeast(1000L),
                waveformAmplitudes = computeRealtimeWaveform(
                    songId = song.id,
                    positionMs = 0L,
                    gainsDb = if (it.equalizerEnabled) it.customBandGainsDb else EqualizerPreset.FLAT.bandGainsDb,
                    bassBoost = if (it.equalizerEnabled) it.bassBoostStrength else 0,
                    isPlaying = false
                )
            )
        }
        startMediaPlayerForSong(song, autoStart = false)
    }

    fun playSong(
        song: SongEntity,
        contextQueue: List<SongEntity> = listOf(song),
        continuationQueue: List<SongEntity> = emptyList(),
        continuationLabel: String? = null
    ) {
        // If clicking on the song that is already playing, take no action
        if (_state.value.currentSong?.id == song.id && _state.value.isPlaying) {
            return
        }

        // Strictly isolate music queues from recording folder queues so recordings never play between music
        val isolatedPrimary = if (song.recordingCategory == null) {
            contextQueue.filter { it.recordingCategory == null }
        } else {
            contextQueue.filter { it.recordingCategory == song.recordingCategory }
        }
        val baseQueue = if (isolatedPrimary.isEmpty()) listOf(song) else isolatedPrimary
        val primaryIds = baseQueue.map { it.id }.toSet()

        val isolatedContinuation = if (song.recordingCategory == null) {
            continuationQueue.filter { it.recordingCategory == null && it.id !in primaryIds }
        } else {
            emptyList()
        }

        val orderedPrimary = if (_state.value.shuffleEnabled && baseQueue.size > 1) {
            val others = baseQueue.filter { it.id != song.id }.shuffled()
            listOf(song) + others
        } else {
            baseQueue
        }
        val orderedContinuation = if (_state.value.shuffleEnabled && isolatedContinuation.size > 1) {
            isolatedContinuation.shuffled()
        } else {
            isolatedContinuation
        }

        basePrimaryQueue = orderedPrimary
        pendingContinuationQueue = orderedContinuation
        currentContinuationDescription = continuationLabel?.takeIf { orderedContinuation.isNotEmpty() }

        val activeRepeatMode = _state.value.repeatMode
        val includeContinuation = activeRepeatMode == RepeatMode.OFF && orderedContinuation.isNotEmpty()
        val finalQueue = when (activeRepeatMode) {
            RepeatMode.ONE -> listOf(song)
            RepeatMode.ALL -> orderedPrimary
            RepeatMode.OFF -> if (includeContinuation) {
                orderedPrimary + orderedContinuation
            } else {
                orderedPrimary
            }
        }
        val targetIndex = finalQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        _state.update {
            it.copy(
                currentSong = song,
                queue = finalQueue,
                currentQueueIndex = targetIndex,
                autoContinuationLabel = if (includeContinuation) currentContinuationDescription else null,
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = song.durationMs.coerceAtLeast(1000L),
                waveformAmplitudes = computeRealtimeWaveform(
                    songId = song.id,
                    positionMs = 0L,
                    gainsDb = if (it.equalizerEnabled) it.customBandGainsDb else EqualizerPreset.FLAT.bandGainsDb,
                    bassBoost = if (it.equalizerEnabled) it.bassBoostStrength else 0,
                    isPlaying = true
                )
            )
        }
        startMediaPlayerForSong(song, autoStart = true)
    }

    private fun configureDataSourceWithFallbacks(mp: MediaPlayer, song: SongEntity, parsedUri: Uri): Boolean {
        val rawUriString = song.uriString.trim()
        val isLocalFileScheme = parsedUri.scheme.equals("file", ignoreCase = true) || rawUriString.startsWith("/")

        if (isLocalFileScheme) {
            val path = if (rawUriString.startsWith("/")) rawUriString else parsedUri.path
            if (!path.isNullOrBlank()) {
                var localFile = File(path)
                if ((!localFile.exists() || localFile.length() <= 44L) &&
                    (song.sourceType == "STUDIO_SAMPLE" || song.sourceType == "RECORDING_SAMPLE")
                ) {
                    localFile = StudioAudioGenerator.ensureLocalSampleFileExists(
                        context = context,
                        targetFile = localFile,
                        durationMs = song.durationMs
                    )
                }
                if (localFile.exists() && localFile.length() > 0L) {
                    val fdSuccess = runCatching {
                        FileInputStream(localFile).use { fis ->
                            mp.setDataSource(fis.fd, 0L, localFile.length())
                        }
                        true
                    }.getOrDefault(false)
                    if (fdSuccess) return true

                    val pathSuccess = runCatching {
                        mp.setDataSource(localFile.absolutePath)
                        true
                    }.getOrDefault(false)
                    if (pathSuccess) return true
                }
            }
        }

        // Content URI or general URI resolution with multiple fallbacks across OEM devices
        val afdSuccess = runCatching {
            context.contentResolver.openAssetFileDescriptor(parsedUri, "r")?.use { afd ->
                if (afd.declaredLength < 0L) {
                    mp.setDataSource(afd.fileDescriptor)
                } else {
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.declaredLength)
                }
                true
            } ?: false
        }.getOrDefault(false)
        if (afdSuccess) return true

        val pfdSuccess = runCatching {
            context.contentResolver.openFileDescriptor(parsedUri, "r")?.use { pfd ->
                mp.setDataSource(pfd.fileDescriptor)
                true
            } ?: false
        }.getOrDefault(false)
        if (pfdSuccess) return true

        return runCatching {
            mp.setDataSource(context, parsedUri)
            true
        }.getOrDefault(false)
    }

    private fun startMediaPlayerForSong(
        song: SongEntity,
        autoStart: Boolean = true,
        skipHardwareEffects: Boolean = false
    ) {
        releasePlayerOnly()
        isPlayerPrepared = false
        shouldPlayWhenPrepared = autoStart
        pendingSeekPositionMs = null

        val parsedUri = runCatching { Uri.parse(song.uriString) }.getOrNull()
        if (parsedUri == null) {
            _state.update {
                it.copy(
                    currentSong = song,
                    isPlaying = autoStart,
                    currentPositionMs = 0L,
                    durationMs = song.durationMs.coerceAtLeast(15_000L)
                )
            }
            if (autoStart) {
                startProgressTicker()
                notifyForegroundService(isPlaying = true)
            }
            return
        }

        try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(playbackAudioAttributes)
            runCatching {
                mp.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            }

            val dataSourceConfigured = configureDataSourceWithFallbacks(mp, song, parsedUri)
            if (!dataSourceConfigured) {
                runCatching { mp.release() }
                throw IllegalStateException("Unable to configure audio data source for ${song.uriString}")
            }

            mp.setOnPreparedListener { player ->
                isPlayerPrepared = true
                runCatching { player.setVolume(1.0f, 1.0f) }

                val realDuration = runCatching { player.duration.toLong() }
                    .getOrNull()
                    ?.takeIf { it > 0L }
                    ?: song.durationMs

                val seekTarget = pendingSeekPositionMs
                if (seekTarget != null && seekTarget > 0L) {
                    runCatching { player.seekTo(seekTarget.coerceAtMost(realDuration).toInt()) }
                    pendingSeekPositionMs = null
                }

                if (shouldPlayWhenPrepared) {
                    requestMediaAudioFocus()
                    runCatching { player.start() }
                    if (!skipHardwareEffects) {
                        attachAudioEffects(player.audioSessionId)
                    }
                    _state.update {
                        it.copy(
                            isPlaying = true,
                            durationMs = realDuration.coerceAtLeast(1000L),
                            currentPositionMs = seekTarget ?: 0L
                        )
                    }
                    startProgressTicker()
                    notifyForegroundService(isPlaying = true)
                } else {
                    if (!skipHardwareEffects) {
                        attachAudioEffects(player.audioSessionId)
                    }
                    _state.update {
                        it.copy(
                            isPlaying = false,
                            durationMs = realDuration.coerceAtLeast(1000L),
                            currentPositionMs = seekTarget ?: 0L
                        )
                    }
                }
            }

            mp.setOnCompletionListener {
                if (isPlayerPrepared) {
                    handleTrackCompleted()
                }
            }

            mp.setOnErrorListener { _, _, _ ->
                isPlayerPrepared = false
                if (!skipHardwareEffects) {
                    // Retry once without hardware DSP effects if the device's audio HAL rejected effect attachment
                    scope.launch {
                        startMediaPlayerForSong(
                            song = song,
                            autoStart = shouldPlayWhenPrepared,
                            skipHardwareEffects = true
                        )
                    }
                    return@setOnErrorListener true
                }
                if (shouldPlayWhenPrepared) {
                    _state.update {
                        it.copy(
                            isPlaying = true,
                            durationMs = song.durationMs.coerceAtLeast(15_000L)
                        )
                    }
                    startProgressTicker()
                    notifyForegroundService(isPlaying = true)
                }
                true
            }

            mediaPlayer = mp
            mp.prepareAsync()
        } catch (_: Exception) {
            isPlayerPrepared = false
            _state.update {
                it.copy(
                    isPlaying = autoStart,
                    durationMs = song.durationMs.coerceAtLeast(15_000L),
                    currentPositionMs = 0L
                )
            }
            if (autoStart) {
                startProgressTicker()
                notifyForegroundService(isPlaying = true)
            }
        }
    }

    private fun notifyForegroundService(isPlaying: Boolean) {
        MusicPlaybackService.syncPlaybackNotification(context, isPlaying)
    }

    private fun attachAudioEffects(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        val currentState = _state.value
        val hasActiveEqCurve = currentState.selectedPreset != EqualizerPreset.FLAT ||
            currentState.customBandGainsDb.any { abs(it) > 0.05f }

        runCatching {
            val existingEq = equalizer
            val eqInstance = if (existingEq != null && equalizerSessionId == audioSessionId) {
                existingEq
            } else {
                runCatching { existingEq?.release() }
                equalizerSessionId = audioSessionId
                Equalizer(0, audioSessionId).also { equalizer = it }
            }
            eqInstance.enabled = currentState.equalizerEnabled && hasActiveEqCurve
            if (currentState.equalizerEnabled) {
                applyEqualizerBands(currentState.customBandGainsDb)
            }
        }
        runCatching {
            val existingBb = bassBoost
            val bbInstance = if (existingBb != null && bassBoostSessionId == audioSessionId) {
                existingBb
            } else {
                runCatching { existingBb?.release() }
                bassBoostSessionId = audioSessionId
                BassBoost(0, audioSessionId).also { bassBoost = it }
            }
            val shouldEnableBass = currentState.equalizerEnabled && currentState.bassBoostStrength > 0
            bbInstance.enabled = shouldEnableBass
            if (bbInstance.strengthSupported && shouldEnableBass) {
                bbInstance.setStrength(currentState.bassBoostStrength.coerceIn(0, 1000).toShort())
            }
        }
    }

    private fun applyEqualizerBands(gainsDb: List<Float>) {
        val eq = equalizer ?: return
        runCatching {
            val currentState = _state.value
            val hasActiveEqCurve = currentState.selectedPreset != EqualizerPreset.FLAT ||
                gainsDb.any { abs(it) > 0.05f }
            eq.enabled = currentState.equalizerEnabled && hasActiveEqCurve
            val numBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minMb = range[0].toInt()
            val maxMb = range[1].toInt()

            for (i in 0 until minOf(numBands, gainsDb.size)) {
                val targetMb = (gainsDb[i] * 100f).toInt().coerceIn(minMb, maxMb).toShort()
                eq.setBandLevel(i.toShort(), targetMb)
            }
        }
    }

    fun togglePlayPause() {
        val current = _state.value
        val activeSong = current.currentSong ?: return
        val mp = mediaPlayer

        // If MediaPlayer is still in PREPARING state (state 4), never call start()/pause() directly!
        if (mp != null && !isPlayerPrepared) {
            val nextPlayState = !shouldPlayWhenPrepared
            shouldPlayWhenPrepared = nextPlayState
            _state.update { it.copy(isPlaying = nextPlayState) }
            if (!nextPlayState) {
                progressJob?.cancel()
            }
            notifyForegroundService(isPlaying = nextPlayState)
            return
        }

        if (current.isPlaying) {
            shouldPlayWhenPrepared = false
            if (isPlayerPrepared && mp != null) {
                runCatching {
                    if (mp.isPlaying) {
                        mp.pause()
                    }
                }
            }
            _state.update { it.copy(isPlaying = false) }
            progressJob?.cancel()
            notifyForegroundService(isPlaying = false)
        } else {
            shouldPlayWhenPrepared = true
            if (isPlayerPrepared && mp != null) {
                requestMediaAudioFocus()
                runCatching {
                    mp.setVolume(1.0f, 1.0f)
                    mp.start()
                }
                _state.update { it.copy(isPlaying = true) }
                startProgressTicker()
                notifyForegroundService(isPlaying = true)
            } else {
                startMediaPlayerForSong(activeSong, autoStart = true)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _state.value.durationMs)
        if (isPlayerPrepared && mediaPlayer != null) {
            runCatching { mediaPlayer?.seekTo(clamped.toInt()) }
        } else {
            pendingSeekPositionMs = clamped
        }
        _state.update { it.copy(currentPositionMs = clamped) }
        notifyForegroundService(isPlaying = _state.value.isPlaying)
    }

    fun skipNext() {
        val current = _state.value
        if (current.repeatMode == RepeatMode.ONE && basePrimaryQueue.size > 1) {
            val activeId = current.currentSong?.id
            val idxInPrimary = basePrimaryQueue.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
            val nextPrimaryIdx = (idxInPrimary + 1) % basePrimaryQueue.size
            val nextSong = basePrimaryQueue[nextPrimaryIdx]
            _state.update {
                it.copy(
                    currentSong = nextSong,
                    queue = listOf(nextSong),
                    currentQueueIndex = 0,
                    isPlaying = true,
                    currentPositionMs = 0L,
                    durationMs = nextSong.durationMs.coerceAtLeast(1000L)
                )
            }
            startMediaPlayerForSong(nextSong, autoStart = true)
            return
        }
        if (current.queue.isEmpty()) return
        val nextIndex = when {
            current.currentQueueIndex + 1 < current.queue.size -> current.currentQueueIndex + 1
            current.repeatMode == RepeatMode.ALL -> 0
            else -> 0
        }
        val nextSong = current.queue[nextIndex]
        _state.update {
            it.copy(
                currentSong = nextSong,
                currentQueueIndex = nextIndex,
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = nextSong.durationMs.coerceAtLeast(1000L)
            )
        }
        startMediaPlayerForSong(nextSong, autoStart = true)
    }

    fun skipPrevious() {
        val current = _state.value
        if (current.currentPositionMs > 3500L) {
            seekTo(0L)
            return
        }
        if (current.repeatMode == RepeatMode.ONE && basePrimaryQueue.size > 1) {
            val activeId = current.currentSong?.id
            val idxInPrimary = basePrimaryQueue.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
            val prevPrimaryIdx = if (idxInPrimary - 1 >= 0) idxInPrimary - 1 else basePrimaryQueue.lastIndex
            val prevSong = basePrimaryQueue[prevPrimaryIdx]
            _state.update {
                it.copy(
                    currentSong = prevSong,
                    queue = listOf(prevSong),
                    currentQueueIndex = 0,
                    isPlaying = true,
                    currentPositionMs = 0L,
                    durationMs = prevSong.durationMs.coerceAtLeast(1000L)
                )
            }
            startMediaPlayerForSong(prevSong, autoStart = true)
            return
        }
        if (current.queue.isEmpty()) return
        val prevIndex = if (current.currentQueueIndex - 1 >= 0) {
            current.currentQueueIndex - 1
        } else {
            current.queue.lastIndex
        }
        val prevSong = current.queue[prevIndex]
        _state.update {
            it.copy(
                currentSong = prevSong,
                currentQueueIndex = prevIndex,
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = prevSong.durationMs.coerceAtLeast(1000L)
            )
        }
        startMediaPlayerForSong(prevSong, autoStart = true)
    }

    private fun handleTrackCompleted() {
        val current = _state.value
        if (current.stopAtEndOfTrack) {
            shouldPlayWhenPrepared = false
            _state.update {
                it.copy(
                    isPlaying = false,
                    currentPositionMs = 0L,
                    stopAtEndOfTrack = false,
                    sleepTimerRemainingMs = null
                )
            }
            progressJob?.cancel()
            notifyForegroundService(isPlaying = false)
            return
        }

        when (current.repeatMode) {
            RepeatMode.ONE -> {
                if (isPlayerPrepared && mediaPlayer != null) {
                    runCatching {
                        mediaPlayer?.seekTo(0)
                        mediaPlayer?.start()
                    }
                    _state.update { it.copy(isPlaying = true, currentPositionMs = 0L) }
                    startProgressTicker()
                    notifyForegroundService(isPlaying = true)
                } else {
                    current.currentSong?.let { startMediaPlayerForSong(it, autoStart = true) }
                }
            }
            RepeatMode.ALL -> {
                skipNext()
            }
            RepeatMode.OFF -> {
                if (current.currentQueueIndex + 1 < current.queue.size) {
                    skipNext()
                } else {
                    shouldPlayWhenPrepared = false
                    if (isPlayerPrepared && mediaPlayer != null) {
                        runCatching { mediaPlayer?.seekTo(0) }
                    }
                    _state.update { it.copy(isPlaying = false, currentPositionMs = 0L) }
                    progressJob?.cancel()
                    notifyForegroundService(isPlaying = false)
                }
            }
        }
    }

    fun toggleShuffle() {
        val current = _state.value
        val newShuffle = !current.shuffleEnabled
        val activeSong = current.currentSong
        val updatedQueue = if (newShuffle && current.queue.size > 1 && activeSong != null) {
            listOf(activeSong) + current.queue.filter { it.id != activeSong.id }.shuffled()
        } else {
            current.queue
        }
        val newIndex = if (activeSong != null) {
            updatedQueue.indexOfFirst { it.id == activeSong.id }.coerceAtLeast(0)
        } else {
            current.currentQueueIndex
        }
        _state.update {
            it.copy(
                shuffleEnabled = newShuffle,
                queue = updatedQueue,
                currentQueueIndex = newIndex
            )
        }
    }

    fun cycleRepeatMode() {
        val current = _state.value
        val nextMode = when (current.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        fxPrefs.edit().putString(KEY_REPEAT_MODE, nextMode.name).apply()

        val activeSong = current.currentSong
        val primaryList = basePrimaryQueue.ifEmpty {
            if (activeSong != null) listOf(activeSong) else current.queue
        }
        val updatedQueue = when (nextMode) {
            RepeatMode.ONE -> {
                // When Repeat One is active, do NOT keep other songs in the Up Next Queue
                if (activeSong != null) listOf(activeSong) else emptyList()
            }
            RepeatMode.ALL -> {
                // When Repeat All is active, include the primary context songs (without cross-folder continuation)
                if (activeSong != null && primaryList.none { it.id == activeSong.id }) {
                    listOf(activeSong) + primaryList
                } else {
                    primaryList
                }
            }
            RepeatMode.OFF -> {
                // When Repeat is OFF, include primary context songs + any auto-continuation songs
                val existingIds = primaryList.map { it.id }.toSet()
                val combined = primaryList + pendingContinuationQueue.filter { it.id !in existingIds }
                if (activeSong != null && combined.none { it.id == activeSong.id }) {
                    listOf(activeSong) + combined
                } else {
                    combined
                }
            }
        }
        val newIndex = if (activeSong != null) {
            updatedQueue.indexOfFirst { it.id == activeSong.id }.coerceAtLeast(0)
        } else {
            0
        }
        val includeContinuation = nextMode == RepeatMode.OFF && pendingContinuationQueue.isNotEmpty()
        _state.update {
            it.copy(
                repeatMode = nextMode,
                queue = updatedQueue,
                currentQueueIndex = newIndex,
                autoContinuationLabel = if (includeContinuation) currentContinuationDescription else null
            )
        }
    }

    fun playFromQueueIndex(index: Int) {
        val queue = _state.value.queue
        if (index !in queue.indices) return
        val target = queue[index]
        if (_state.value.currentSong?.id == target.id && _state.value.isPlaying) {
            return
        }
        _state.update {
            it.copy(
                currentSong = target,
                currentQueueIndex = index,
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = target.durationMs.coerceAtLeast(1000L)
            )
        }
        startMediaPlayerForSong(target, autoStart = true)
    }

    fun addSongNextInQueue(song: SongEntity) {
        val current = _state.value
        val activeCategory = current.currentSong?.recordingCategory
        // Never allow mixing recordings into a music queue or music into a recording queue
        if (current.queue.isEmpty() || activeCategory != song.recordingCategory) {
            playSong(song, listOf(song))
            return
        }
        val mutable = current.queue.toMutableList()
        val insertIdx = (current.currentQueueIndex + 1).coerceIn(0, mutable.size)
        mutable.add(insertIdx, song)
        _state.update { it.copy(queue = mutable) }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val current = _state.value
        if (fromIndex !in current.queue.indices || toIndex !in current.queue.indices) return
        val mutable = current.queue.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        val activeId = current.currentSong?.id
        val newCurrentIdx = if (activeId != null) {
            mutable.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
        } else {
            current.currentQueueIndex
        }
        _state.update {
            it.copy(
                queue = mutable,
                currentQueueIndex = newCurrentIdx
            )
        }
    }

    fun removeFromQueue(index: Int) {
        val current = _state.value
        if (index !in current.queue.indices || current.queue.size <= 1) return
        val removedSong = current.queue[index]
        val mutable = current.queue.toMutableList().apply { removeAt(index) }
        if (removedSong.id == current.currentSong?.id) {
            val nextIdx = index.coerceAtMost(mutable.lastIndex)
            val nextSong = mutable[nextIdx]
            val wasPlaying = current.isPlaying
            _state.update {
                it.copy(
                    queue = mutable,
                    currentQueueIndex = nextIdx,
                    currentSong = nextSong,
                    isPlaying = wasPlaying
                )
            }
            startMediaPlayerForSong(nextSong, autoStart = wasPlaying)
        } else {
            val newCurrentIdx = mutable.indexOfFirst { it.id == current.currentSong?.id }.coerceAtLeast(0)
            _state.update {
                it.copy(
                    queue = mutable,
                    currentQueueIndex = newCurrentIdx
                )
            }
        }
    }

    // Equalizer & Audio FX (Globally locked across all song changes & persisted)
    fun setEqualizerEnabled(enabled: Boolean) {
        _state.update { current ->
            val updatedAmps = computeRealtimeWaveform(
                songId = current.currentSong?.id ?: 1L,
                positionMs = current.currentPositionMs,
                gainsDb = if (enabled) current.customBandGainsDb else EqualizerPreset.FLAT.bandGainsDb,
                bassBoost = if (enabled) current.bassBoostStrength else 0,
                isPlaying = current.isPlaying
            )
            current.copy(
                equalizerEnabled = enabled,
                waveformAmplitudes = updatedAmps
            )
        }
        val activeSessionId = if (isPlayerPrepared) runCatching { mediaPlayer?.audioSessionId ?: -1 }.getOrDefault(-1) else -1
        if (activeSessionId > 0) {
            attachAudioEffects(activeSessionId)
        } else {
            runCatching { equalizer?.enabled = enabled }
            runCatching { bassBoost?.enabled = enabled && _state.value.bassBoostStrength > 0 }
        }
        persistStudioFxState(enabled = enabled)
    }

    fun selectEqualizerPreset(preset: EqualizerPreset) {
        val targetBands = if (preset == EqualizerPreset.CUSTOM) {
            userCustomGainsDb
        } else {
            preset.bandGainsDb
        }
        val activeTuning = if (preset == EqualizerPreset.CUSTOM) {
            _state.value.savedCustomTunings.find { it.id == _state.value.activeCustomTuningId }
        } else {
            null
        }
        _state.update { current ->
            val updatedAmps = computeRealtimeWaveform(
                songId = current.currentSong?.id ?: 1L,
                positionMs = current.currentPositionMs,
                gainsDb = if (current.equalizerEnabled) targetBands else EqualizerPreset.FLAT.bandGainsDb,
                bassBoost = if (current.equalizerEnabled) current.bassBoostStrength else 0,
                isPlaying = current.isPlaying
            )
            current.copy(
                selectedPreset = preset,
                customBandGainsDb = targetBands,
                activeCustomTuningId = activeTuning?.id,
                activeCustomTuningName = activeTuning?.name,
                waveformAmplitudes = updatedAmps
            )
        }
        val activeSessionId = if (isPlayerPrepared) runCatching { mediaPlayer?.audioSessionId ?: -1 }.getOrDefault(-1) else -1
        if (activeSessionId > 0 && equalizer == null) {
            attachAudioEffects(activeSessionId)
        } else {
            applyEqualizerBands(targetBands)
        }
        persistStudioFxState(
            preset = preset,
            bands = targetBands,
            activeTuningId = activeTuning?.id
        )
    }

    fun updateBandGain(bandIndex: Int, gainDb: Float) {
        val currentBands = _state.value.customBandGainsDb.toMutableList()
        if (bandIndex !in currentBands.indices) return
        currentBands[bandIndex] = gainDb.coerceIn(-12f, 12f)
        userCustomGainsDb = currentBands.toList()
        _state.update { current ->
            val updatedAmps = computeRealtimeWaveform(
                songId = current.currentSong?.id ?: 1L,
                positionMs = current.currentPositionMs,
                gainsDb = if (current.equalizerEnabled) currentBands else EqualizerPreset.FLAT.bandGainsDb,
                bassBoost = if (current.equalizerEnabled) current.bassBoostStrength else 0,
                isPlaying = current.isPlaying
            )
            current.copy(
                selectedPreset = EqualizerPreset.CUSTOM,
                customBandGainsDb = currentBands,
                waveformAmplitudes = updatedAmps
            )
        }
        val activeSessionId = if (isPlayerPrepared) runCatching { mediaPlayer?.audioSessionId ?: -1 }.getOrDefault(-1) else -1
        if (activeSessionId > 0 && equalizer == null) {
            attachAudioEffects(activeSessionId)
        } else {
            applyEqualizerBands(currentBands)
        }
        persistStudioFxState(preset = EqualizerPreset.CUSTOM, bands = currentBands)
    }

    fun updateBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _state.update { current ->
            val updatedAmps = computeRealtimeWaveform(
                songId = current.currentSong?.id ?: 1L,
                positionMs = current.currentPositionMs,
                gainsDb = if (current.equalizerEnabled) current.customBandGainsDb else EqualizerPreset.FLAT.bandGainsDb,
                bassBoost = if (current.equalizerEnabled) clamped else 0,
                isPlaying = current.isPlaying
            )
            current.copy(
                bassBoostStrength = clamped,
                waveformAmplitudes = updatedAmps
            )
        }
        val activeSessionId = if (isPlayerPrepared) runCatching { mediaPlayer?.audioSessionId ?: -1 }.getOrDefault(-1) else -1
        if (activeSessionId > 0 && bassBoost == null) {
            attachAudioEffects(activeSessionId)
        } else {
            runCatching {
                val bb = bassBoost
                if (bb != null) {
                    val shouldEnable = _state.value.equalizerEnabled && clamped > 0
                    bb.enabled = shouldEnable
                    if (bb.strengthSupported && shouldEnable) {
                        bb.setStrength(clamped.toShort())
                    }
                }
            }
        }
        persistStudioFxState(bassStrength = clamped)
    }

    fun saveCustomTuning(rawName: String): SavedCustomTuning {
        val current = _state.value
        val existingList = current.savedCustomTunings.toMutableList()
        val trimmedName = rawName.trim().ifBlank {
            current.activeCustomTuningName ?: "Custom Studio #${existingList.size + 1}"
        }
        val bandsToSave = current.customBandGainsDb.takeIf { it.size == 5 } ?: EqualizerPreset.FLAT.bandGainsDb
        val bassToSave = current.bassBoostStrength.coerceIn(0, 1000)

        val existingIndex = existingList.indexOfFirst { it.name.equals(trimmedName, ignoreCase = true) }
        val savedTuning = if (existingIndex >= 0) {
            existingList[existingIndex].copy(
                name = trimmedName,
                bandGainsDb = bandsToSave,
                bassBoostStrength = bassToSave
            ).also { existingList[existingIndex] = it }
        } else {
            SavedCustomTuning(
                id = "tuning_${System.currentTimeMillis()}",
                name = trimmedName,
                bandGainsDb = bandsToSave,
                bassBoostStrength = bassToSave
            ).also { existingList.add(0, it) }
        }

        userCustomGainsDb = bandsToSave
        _state.update { state ->
            state.copy(
                selectedPreset = EqualizerPreset.CUSTOM,
                customBandGainsDb = bandsToSave,
                bassBoostStrength = bassToSave,
                savedCustomTunings = existingList,
                activeCustomTuningId = savedTuning.id,
                activeCustomTuningName = savedTuning.name
            )
        }
        val activeSessionId = if (isPlayerPrepared) runCatching { mediaPlayer?.audioSessionId ?: -1 }.getOrDefault(-1) else -1
        if (activeSessionId > 0) {
            attachAudioEffects(activeSessionId)
        }
        persistStudioFxState(
            preset = EqualizerPreset.CUSTOM,
            bands = bandsToSave,
            bassStrength = bassToSave,
            savedTunings = existingList,
            activeTuningId = savedTuning.id
        )
        return savedTuning
    }

    fun applySavedCustomTuning(tuning: SavedCustomTuning) {
        userCustomGainsDb = tuning.bandGainsDb
        _state.update { current ->
            val updatedAmps = computeRealtimeWaveform(
                songId = current.currentSong?.id ?: 1L,
                positionMs = current.currentPositionMs,
                gainsDb = if (current.equalizerEnabled) tuning.bandGainsDb else EqualizerPreset.FLAT.bandGainsDb,
                bassBoost = if (current.equalizerEnabled) tuning.bassBoostStrength else 0,
                isPlaying = current.isPlaying
            )
            current.copy(
                selectedPreset = EqualizerPreset.CUSTOM,
                customBandGainsDb = tuning.bandGainsDb,
                bassBoostStrength = tuning.bassBoostStrength,
                activeCustomTuningId = tuning.id,
                activeCustomTuningName = tuning.name,
                waveformAmplitudes = updatedAmps
            )
        }
        val activeSessionId = if (isPlayerPrepared) runCatching { mediaPlayer?.audioSessionId ?: -1 }.getOrDefault(-1) else -1
        if (activeSessionId > 0) {
            attachAudioEffects(activeSessionId)
        }
        persistStudioFxState(
            preset = EqualizerPreset.CUSTOM,
            bands = tuning.bandGainsDb,
            bassStrength = tuning.bassBoostStrength,
            activeTuningId = tuning.id
        )
    }

    fun deleteSavedCustomTuning(tuningId: String) {
        val current = _state.value
        val updatedList = current.savedCustomTunings.filterNot { it.id == tuningId }
        val clearedActiveId = if (current.activeCustomTuningId == tuningId) null else current.activeCustomTuningId
        val clearedActiveName = if (current.activeCustomTuningId == tuningId) null else current.activeCustomTuningName
        _state.update {
            it.copy(
                savedCustomTunings = updatedList,
                activeCustomTuningId = clearedActiveId,
                activeCustomTuningName = clearedActiveName
            )
        }
        persistStudioFxState(
            savedTunings = updatedList,
            activeTuningId = clearedActiveId
        )
    }

    fun resetCustomTuningToFlat() {
        val flatBands = EqualizerPreset.FLAT.bandGainsDb
        userCustomGainsDb = flatBands
        _state.update { current ->
            val updatedAmps = computeRealtimeWaveform(
                songId = current.currentSong?.id ?: 1L,
                positionMs = current.currentPositionMs,
                gainsDb = flatBands,
                bassBoost = if (current.equalizerEnabled) current.bassBoostStrength else 0,
                isPlaying = current.isPlaying
            )
            current.copy(
                selectedPreset = EqualizerPreset.CUSTOM,
                customBandGainsDb = flatBands,
                activeCustomTuningId = null,
                activeCustomTuningName = null,
                waveformAmplitudes = updatedAmps
            )
        }
        applyEqualizerBands(flatBands)
        persistStudioFxState(
            preset = EqualizerPreset.CUSTOM,
            bands = flatBands,
            activeTuningId = null
        )
    }

    // Sleep Timer
    fun setSleepTimerMinutes(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _state.update {
                it.copy(sleepTimerRemainingMs = null, stopAtEndOfTrack = false)
            }
            return
        }
        val totalMs = minutes * 60_000L
        _state.update {
            it.copy(sleepTimerRemainingMs = totalMs, stopAtEndOfTrack = false)
        }
        sleepTimerJob = scope.launch {
            var remaining = totalMs
            while (isActive && remaining > 0L) {
                delay(1000L)
                remaining = (remaining - 1000L).coerceAtLeast(0L)
                _state.update { it.copy(sleepTimerRemainingMs = remaining) }
            }
            if (remaining <= 0L) {
                if (_state.value.isPlaying) {
                    togglePlayPause()
                }
                _state.update { it.copy(sleepTimerRemainingMs = null) }
            }
        }
    }

    fun setStopAtEndOfTrack(enabled: Boolean) {
        sleepTimerJob?.cancel()
        _state.update {
            it.copy(
                stopAtEndOfTrack = enabled,
                sleepTimerRemainingMs = null
            )
        }
    }

    fun syncUpdatedSongs(allItems: List<SongEntity>) {
        val songMap = allItems.associateBy { it.id }
        basePrimaryQueue = basePrimaryQueue.mapNotNull { songMap[it.id] }
        pendingContinuationQueue = pendingContinuationQueue.mapNotNull { songMap[it.id] }
        _state.update { current ->
            val updatedCurrent = current.currentSong?.let { songMap[it.id] ?: it }
            val activeCategory = updatedCurrent?.recordingCategory
            val updatedQueue = current.queue
                .mapNotNull { songMap[it.id] }
                .filter { it.recordingCategory == activeCategory }
            current.copy(
                currentSong = updatedCurrent,
                queue = if (updatedQueue.isEmpty() && updatedCurrent != null) listOf(updatedCurrent) else updatedQueue
            )
        }
    }

    private fun computeRealtimeWaveform(
        songId: Long,
        positionMs: Long,
        gainsDb: List<Float>,
        bassBoost: Int,
        isPlaying: Boolean
    ): List<Float> {
        val barCount = 36
        val timeSec = positionMs / 1000.0
        val bassFactor = 1f + (bassBoost / 1000f) * 0.35f
        return List(barCount) { i ->
            val bandIdx = ((i.toFloat() / barCount) * gainsDb.size).toInt().coerceIn(0, (gainsDb.size - 1).coerceAtLeast(0))
            val eqGainBoost = if (gainsDb.isNotEmpty()) {
                1f + (gainsDb[bandIdx] / 24f)
            } else {
                1f
            }
            val lowEndWeight = if (i < 10) bassFactor else 1f
            val seed = (songId + 1L) * 0.37 + i * 0.53
            val wave1 = kotlin.math.sin(timeSec * 5.2 + seed) * 0.34
            val wave2 = kotlin.math.cos(timeSec * 8.7 - i * 0.81) * 0.24
            val envelope = kotlin.math.sin((i.toDouble() / (barCount - 1)) * kotlin.math.PI)
            val raw = if (isPlaying) {
                (0.32 + kotlin.math.abs(wave1 + wave2)) * (0.45 + 0.55 * envelope) * eqGainBoost * lowEndWeight
            } else {
                (0.20 + 0.25 * kotlin.math.abs(kotlin.math.sin(seed))) * (0.5 + 0.5 * envelope) * eqGainBoost * lowEndWeight
            }
            raw.toFloat().coerceIn(0.10f, 1.0f)
        }
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _state.value.isPlaying) {
                delay(120L)
                val mp = mediaPlayer
                val pos = if (isPlayerPrepared && mp != null) {
                    runCatching {
                        if (mp.isPlaying) mp.currentPosition.toLong() else null
                    }.getOrNull()
                } else {
                    null
                }

                val currentSnap = _state.value
                val songId = currentSnap.currentSong?.id ?: 1L
                val activeGains = if (currentSnap.equalizerEnabled) currentSnap.customBandGainsDb else EqualizerPreset.FLAT.bandGainsDb
                val activeBass = if (currentSnap.equalizerEnabled) currentSnap.bassBoostStrength else 0

                if (pos != null) {
                    val clampedPos = pos.coerceAtMost(currentSnap.durationMs)
                    val amplitudes = computeRealtimeWaveform(
                        songId = songId,
                        positionMs = clampedPos,
                        gainsDb = activeGains,
                        bassBoost = activeBass,
                        isPlaying = true
                    )
                    _state.update {
                        it.copy(
                            currentPositionMs = clampedPos,
                            waveformAmplitudes = amplitudes
                        )
                    }
                } else if (!isPlayerPrepared && mp == null) {
                    val nextPos = currentSnap.currentPositionMs + 120L
                    if (nextPos >= currentSnap.durationMs) {
                        handleTrackCompleted()
                    } else {
                        val amplitudes = computeRealtimeWaveform(
                            songId = songId,
                            positionMs = nextPos,
                            gainsDb = activeGains,
                            bassBoost = activeBass,
                            isPlaying = true
                        )
                        _state.update {
                            it.copy(
                                currentPositionMs = nextPos,
                                waveformAmplitudes = amplitudes
                            )
                        }
                    }
                }
            }
        }
    }

    private fun releasePlayerOnly() {
        progressJob?.cancel()
        isPlayerPrepared = false
        shouldPlayWhenPrepared = false
        runCatching {
            equalizer?.enabled = false
            equalizer?.release()
        }
        equalizer = null
        equalizerSessionId = -1
        runCatching {
            bassBoost?.enabled = false
            bassBoost?.release()
        }
        bassBoost = null
        bassBoostSessionId = -1

        val mp = mediaPlayer
        mediaPlayer = null
        if (mp != null) {
            runCatching {
                mp.setOnPreparedListener(null)
                mp.setOnCompletionListener(null)
                mp.setOnErrorListener(null)
                mp.reset()
                mp.release()
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "aura_studio_fx_prefs"
        private const val KEY_EQ_ENABLED = "eq_enabled"
        private const val KEY_EQ_PRESET = "eq_preset"
        private const val KEY_CUSTOM_BANDS = "eq_custom_bands"
        private const val KEY_BASS_STRENGTH = "bass_boost_strength"
        private const val KEY_SAVED_CUSTOM_TUNINGS = "saved_custom_tunings"
        private const val KEY_ACTIVE_CUSTOM_TUNING_ID = "active_custom_tuning_id"
        private const val KEY_REPEAT_MODE = "repeat_mode"

        @Volatile
        private var INSTANCE: AudioPlayerController? = null

        fun getInstance(context: Context): AudioPlayerController {
            return INSTANCE ?: synchronized(this) {
                val instance = AudioPlayerController(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
