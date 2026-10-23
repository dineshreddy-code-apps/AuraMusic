package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.SongEntity
import java.io.File
import java.util.Locale

private val artworkMemoryCache = LruCache<String, ImageBitmap>(64)

@Composable
fun rememberArtworkBitmap(artworkPath: String?): ImageBitmap? {
    return remember(artworkPath) {
        if (artworkPath.isNullOrBlank()) {
            null
        } else {
            artworkMemoryCache.get(artworkPath) ?: runCatching {
                val file = File(artworkPath)
                if (file.exists() && file.length() > 0L) {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()?.also { bitmap ->
                        artworkMemoryCache.put(artworkPath, bitmap)
                    }
                } else {
                    null
                }
            }.getOrNull()
        }
    }
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

@Composable
fun AuraBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val shape = RoundedCornerShape(13.dp)
    Surface(
        color = Color(0xFFF7F7F7),
        shape = shape,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        modifier = modifier
            .size(size)
            .clip(shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                shape = shape
            )
            .testTag("aura_brand_logo")
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_aura_logo),
            contentDescription = "Aura Music Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun AlbumArtworkView(
    colorHex: Long,
    title: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    isPlayingIndicator: Boolean = false,
    artworkPath: String? = null
) {
    val baseColor = Color(colorHex)
    val darkShade = Color(
        red = (baseColor.red * 0.32f).coerceIn(0f, 1f),
        green = (baseColor.green * 0.32f).coerceIn(0f, 1f),
        blue = (baseColor.blue * 0.38f).coerceIn(0f, 1f),
        alpha = 1f
    )
    val highlightColor = Color(
        red = (baseColor.red + (1f - baseColor.red) * 0.35f).coerceIn(0f, 1f),
        green = (baseColor.green + (1f - baseColor.green) * 0.35f).coerceIn(0f, 1f),
        blue = (baseColor.blue + (1f - baseColor.blue) * 0.35f).coerceIn(0f, 1f),
        alpha = 1f
    )

    val shape = RoundedCornerShape(cornerRadius)
    val extractedBitmap = rememberArtworkBitmap(artworkPath)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(highlightColor, baseColor, darkShade)
                )
            )
            .border(
                width = 0.8.dp,
                color = Color.White.copy(alpha = 0.16f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (extractedBitmap != null) {
            Image(
                bitmap = extractedBitmap,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val minDim = minOf(size.width, size.height)
                val center = Offset(size.width * 0.5f, size.height * 0.5f)

                // Subtle vinyl / acoustic rings
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = minDim * 0.38f,
                    center = center,
                    style = Stroke(width = minDim * 0.025f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = minDim * 0.27f,
                    center = center,
                    style = Stroke(width = minDim * 0.018f)
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.22f),
                    radius = minDim * 0.13f,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.75f),
                    radius = minDim * 0.035f,
                    center = center
                )
            }
        }

        if (isPlayingIndicator) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.36f)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedEqualizerBars(
                    barColor = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else if (extractedBitmap == null) {
            val monogram = title.trim().take(1).uppercase(Locale.US)
            if (monogram.isNotEmpty()) {
                Text(
                    text = monogram,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.82f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                )
            }
        }
    }
}

@Composable
fun AnimatedEqualizerBars(
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq_bars")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(tween(340, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(510, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar3"
    )

    Canvas(modifier = modifier) {
        val barWidth = size.width / 5f
        val gap = barWidth * 0.8f
        val heights = listOf(bar1, bar2, bar3)
        heights.forEachIndexed { idx, fraction ->
            val h = size.height * fraction
            val left = idx * (barWidth + gap) + barWidth * 0.2f
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun SingerAvatarView(
    colorHex: Long,
    singerName: String,
    modifier: Modifier = Modifier,
    artworkPath: String? = null
) {
    val baseColor = Color(colorHex)
    val deepTone = Color(
        red = (baseColor.red * 0.4f).coerceIn(0f, 1f),
        green = (baseColor.green * 0.4f).coerceIn(0f, 1f),
        blue = (baseColor.blue * 0.45f).coerceIn(0f, 1f),
        alpha = 1f
    )
    val initials = singerName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1).uppercase(Locale.US) }
        .ifBlank { "S" }
    val extractedBitmap = rememberArtworkBitmap(artworkPath)

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(baseColor, deepTone)
                )
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (extractedBitmap != null) {
            Image(
                bitmap = extractedBitmap,
                contentDescription = singerName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SongRowItem(
    song: SongEntity,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onSongClick: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenOptions: () -> Unit,
    modifier: Modifier = Modifier,
    trackIndexNumber: Int? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongPressSelect: () -> Unit = {},
    onToggleSelect: () -> Unit = {}
) {
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        isCurrentSong -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else -> Color.Transparent
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 1.2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(14.dp)
                    )
                } else {
                    Modifier
                }
            )
            .longPressMoreThanOneSecond(
                key = song.id to isSelectionMode,
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        onSongClick()
                    }
                },
                onLongPressMoreThanOneSecond = onLongPressSelect
            )
            .testTag("song_row_${song.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode || isSelected) {
                GoogleFilesSelectionIndicator(
                    isSelected = isSelected,
                    size = 24.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            if (trackIndexNumber != null) {
                Text(
                    text = trackIndexNumber.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isCurrentSong) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.width(26.dp)
                )
            } else {
                AlbumArtworkView(
                    colorHex = song.colorHex,
                    title = song.albumName,
                    cornerRadius = 10.dp,
                    isPlayingIndicator = isCurrentSong && isPlaying,
                    artworkPath = song.artworkPath,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrentSong) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${song.singerName} • ${song.albumName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formatDuration(song.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            IconButton(
                onClick = onToggleLike,
                modifier = Modifier.testTag("like_button_${song.id}")
            ) {
                Icon(
                    imageVector = if (song.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (song.isLiked) "Remove from Liked Songs" else "Add to Liked Songs",
                    tint = if (song.isLiked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            IconButton(
                onClick = onOpenOptions,
                modifier = Modifier.testTag("song_options_${song.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Song options and folder organization",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
