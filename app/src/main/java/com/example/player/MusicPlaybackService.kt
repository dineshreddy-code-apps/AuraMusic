package com.example.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState as SystemPlaybackState
import android.os.Build
import android.os.IBinder
import com.example.MainActivity
import com.example.R
import com.example.data.SongEntity
import java.io.File

class MusicPlaybackService : Service() {

    private var mediaSession: MediaSession? = null
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureNotificationChannel()

        val controller = AudioPlayerController.getInstance(applicationContext)
        mediaSession = MediaSession(this, "AuraMusicPlaybackSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    if (!controller.state.value.isPlaying) {
                        controller.togglePlayPause()
                    }
                }

                override fun onPause() {
                    if (controller.state.value.isPlaying) {
                        controller.togglePlayPause()
                    }
                }

                override fun onSkipToNext() {
                    controller.skipNext()
                }

                override fun onSkipToPrevious() {
                    controller.skipPrevious()
                }

                override fun onSeekTo(pos: Long) {
                    controller.seekTo(pos)
                }

                override fun onStop() {
                    if (controller.state.value.isPlaying) {
                        controller.togglePlayPause()
                    }
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val controller = AudioPlayerController.getInstance(applicationContext)
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> controller.togglePlayPause()
            ACTION_NEXT -> controller.skipNext()
            ACTION_PREVIOUS -> controller.skipPrevious()
            ACTION_STOP -> {
                if (controller.state.value.isPlaying) {
                    controller.togglePlayPause()
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val currentState = controller.state.value
        val currentSong = currentState.currentSong
        if (currentSong != null) {
            updateMediaSession(currentSong, currentState)
            val notification = buildMediaNotification(currentSong, currentState.isPlaying)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }

        return START_STICKY
    }

    private fun updateMediaSession(song: SongEntity, state: PlaybackState) {
        val session = mediaSession ?: return
        val artBitmap = createAlbumArtworkBitmap(song.colorHex, song.artworkPath)

        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, song.singerName)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, song.albumName)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, state.durationMs.coerceAtLeast(1000L))
            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artBitmap)
            .build()
        session.setMetadata(metadata)

        val actions = SystemPlaybackState.ACTION_PLAY or
            SystemPlaybackState.ACTION_PAUSE or
            SystemPlaybackState.ACTION_PLAY_PAUSE or
            SystemPlaybackState.ACTION_SKIP_TO_NEXT or
            SystemPlaybackState.ACTION_SKIP_TO_PREVIOUS or
            SystemPlaybackState.ACTION_SEEK_TO or
            SystemPlaybackState.ACTION_STOP

        val playbackStateInt = if (state.isPlaying) {
            SystemPlaybackState.STATE_PLAYING
        } else {
            SystemPlaybackState.STATE_PAUSED
        }

        val sysState = SystemPlaybackState.Builder()
            .setActions(actions)
            .setState(
                playbackStateInt,
                state.currentPositionMs.coerceAtLeast(0L),
                if (state.isPlaying) 1.0f else 0f
            )
            .build()
        session.setPlaybackState(sysState)
    }

    private fun buildMediaNotification(song: SongEntity, isPlaying: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevAction = Notification.Action.Builder(
            android.R.drawable.ic_media_previous,
            "Previous",
            buildServicePendingIntent(ACTION_PREVIOUS, 101)
        ).build()

        val playPauseAction = Notification.Action.Builder(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (isPlaying) "Pause" else "Play",
            buildServicePendingIntent(ACTION_PLAY_PAUSE, 102)
        ).build()

        val nextAction = Notification.Action.Builder(
            android.R.drawable.ic_media_next,
            "Next",
            buildServicePendingIntent(ACTION_NEXT, 103)
        ).build()

        val stopAction = Notification.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop",
            buildServicePendingIntent(ACTION_STOP, 104)
        ).build()

        val mediaStyle = Notification.MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val largeIcon = createAlbumArtworkBitmap(song.colorHex, song.artworkPath)

        return builder
            .setSmallIcon(R.drawable.ic_stat_aura_logo)
            .setLargeIcon(largeIcon)
            .setContentTitle(song.title)
            .setContentText(song.singerName)
            .setSubText(song.albumName)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(buildServicePendingIntent(ACTION_STOP, 105))
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setColor((song.colorHex or 0xFF000000L).toInt())
            .setColorized(true)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(stopAction)
            .setStyle(mediaStyle)
            .build()
    }

    private fun buildServicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, MusicPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createAlbumArtworkBitmap(colorHex: Long, artworkPath: String? = null): Bitmap {
        if (!artworkPath.isNullOrBlank()) {
            runCatching {
                val file = File(artworkPath)
                if (file.exists() && file.length() > 0L) {
                    BitmapFactory.decodeFile(file.absolutePath)?.let { decoded ->
                        return decoded
                    }
                }
            }
        }
        val sizePx = 256
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val baseColor = (colorHex or 0xFF000000L).toInt()
        val darkColor = 0xFF121218.toInt()

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                sizePx.toFloat(),
                sizePx.toFloat(),
                baseColor,
                darkColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = 0x33FFFFFF
        }
        val center = sizePx / 2f
        canvas.drawCircle(center, center, sizePx * 0.36f, ringPaint)
        canvas.drawCircle(center, center, sizePx * 0.24f, ringPaint)

        val centerDiscPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0x44000000
        }
        canvas.drawCircle(center, center, sizePx * 0.12f, centerDiscPaint)

        val spindlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0xDDFFFFFF.toInt()
        }
        canvas.drawCircle(center, center, sizePx * 0.035f, spindlePaint)
        return bitmap
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Aura Music Playback",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows currently playing offline song and transport controls"
                    setShowBadge(false)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onDestroy() {
        runCatching {
            mediaSession?.isActive = false
            mediaSession?.release()
        }
        mediaSession = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "aura_music_playback_channel"
        const val NOTIFICATION_ID = 4012
        const val ACTION_PLAY_PAUSE = "com.example.player.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.player.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.player.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.example.player.ACTION_STOP"
        const val ACTION_SYNC_NOTIFICATION = "com.example.player.ACTION_SYNC_NOTIFICATION"

        fun syncPlaybackNotification(context: Context, isPlaying: Boolean) {
            runCatching {
                val intent = Intent(context, MusicPlaybackService::class.java).apply {
                    action = ACTION_SYNC_NOTIFICATION
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isPlaying) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
    }
}
