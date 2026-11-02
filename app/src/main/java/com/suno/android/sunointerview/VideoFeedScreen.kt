package com.suno.android.sunointerview

import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import kotlinx.coroutines.delay

@Composable
fun VideoFeedScreen(
    modifier: Modifier = Modifier,
    feedViewModel: VideoFeedViewModel = viewModel()
) {
    val state by feedViewModel.state.collectAsStateWithLifecycle()
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            state.loading -> LoadingOverlay(progressPercent = null)
            state.error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.error.orEmpty(), modifier = Modifier.padding(24.dp))
                Button(onClick = feedViewModel::loadVideos) { Text("Retry") }
            }
            state.videos.isEmpty() -> Text("No videos available")
            else -> VideoPager(state.videos)
        }
    }
}

@Composable
internal fun VideoPager(videos: List<VideoCard>, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { videos.size })
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            key = { videos[it].url },
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val video = videos[page]
            Box(modifier = Modifier.fillMaxSize()) {
                VideoSurface(
                    video = video,
                    active = pagerState.settledPage == page && !pagerState.isScrollInProgress,
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    "${page + 1} / ${videos.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.TopEnd)
                        .safeDrawingPadding().padding(24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
internal fun VideoSurface(video: VideoCard, active: Boolean, modifier: Modifier = Modifier) {
    val videoUrl = video.url
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    var position by rememberSaveable(videoUrl) { mutableLongStateOf(0L) }
    var shouldPlay by rememberSaveable(videoUrl) { mutableStateOf(true) }
    var completed by rememberSaveable(videoUrl) { mutableStateOf(false) }
    val latestShouldPlay by rememberUpdatedState(shouldPlay)
    var player by remember(videoUrl) { mutableStateOf<ExoPlayer?>(null) }
    var buffering by remember(videoUrl) { mutableStateOf(false) }
    var error by remember(videoUrl) { mutableStateOf<String?>(null) }
    var bufferedPercent by remember(videoUrl) { mutableStateOf<Int?>(null) }

    fun togglePlayback() {
        player?.let { current ->
            if (completed || current.playbackState == Player.STATE_ENDED) {
                completed = false
                position = 0L
                current.seekTo(0L)
                shouldPlay = true
                current.play()
            } else {
                shouldPlay = !current.playWhenReady
                current.playWhenReady = shouldPlay
            }
        }
    }

    LaunchedEffect(player, buffering) {
        val current = player
        while (buffering && current != null) {
            bufferedPercent = if (current.duration > 0L) {
                current.bufferedPercentage.coerceIn(0, 100)
            } else {
                null
            }
            delay(200L)
        }
    }

    DisposableEffect(videoUrl, active, lifecycleOwner) {
        var ownedPlayer: ExoPlayer? = null
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                shouldPlay = playWhenReady
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_ENDED) {
                    completed = true
                    shouldPlay = false
                }
            }

            override fun onPlayerError(playbackError: PlaybackException) {
                buffering = false
                error = "Video unavailable. Tap Retry to try again."
            }
        }

        fun releasePlayer() {
            ownedPlayer?.let { current ->
                position = current.currentPosition
                current.removeListener(listener)
                current.release()
            }
            ownedPlayer = null
            player = null
            buffering = false
            bufferedPercent = null
        }

        fun updatePlayer() {
            if (active && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                if (ownedPlayer == null) {
                    error = null
                    ownedPlayer = ExoPlayer.Builder(context).build().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(C.USAGE_MEDIA)
                                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                                .build(),
                            true
                        )
                        setHandleAudioBecomingNoisy(true)
                        repeatMode = Player.REPEAT_MODE_OFF
                        addListener(listener)
                        setMediaItem(MediaItem.fromUri(videoUrl), position)
                        playWhenReady = latestShouldPlay
                        prepare()
                    }
                    player = ownedPlayer
                }
            } else {
                releasePlayer()
            }
        }

        val observer = LifecycleEventObserver { _, _ -> updatePlayer() }
        lifecycleOwner.lifecycle.addObserver(observer)
        updatePlayer()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            releasePlayer()
        }
    }

    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            update = { view ->
                view.player = player
                view.keepScreenOn = active && shouldPlay && !completed && error == null
            },
            onRelease = { view ->
                view.player = null
                view.keepScreenOn = false
            },
            modifier = Modifier.fillMaxSize()
        )
        if (active && player != null && error == null) {
            Box(
                modifier = Modifier.align(Alignment.Center).size(160.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = if (completed) "Restart" else if (shouldPlay) "Pause" else "Play",
                        onClick = ::togglePlayback
                    )
            )
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                .safeDrawingPadding().padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 24.dp)
        ) {
            FilledIconButton(
                onClick = ::togglePlayback,
                enabled = active && player != null && error == null,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF101824),
                    disabledContainerColor = Color.White.copy(alpha = 0.3f),
                    disabledContentColor = Color.Black.copy(alpha = 0.5f)
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
                    .padding(bottom = 12.dp).size(64.dp)
            ) {
                Icon(
                    imageVector = when {
                        completed -> Icons.Default.Replay
                        shouldPlay -> Icons.Default.Pause
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = when {
                        completed -> "Restart"
                        shouldPlay -> "Pause"
                        else -> "Play"
                    },
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(video.title, color = Color.White, style = MaterialTheme.typography.titleLarge)
            if (video.creator.isNotBlank()) {
                Text(video.creator, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (active) {
            if (error != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error.orEmpty(), color = Color.White, modifier = Modifier.padding(16.dp))
                    Button(onClick = {
                        error = null
                        completed = false
                        shouldPlay = true
                        player?.apply {
                            prepare()
                            play()
                        }
                    }) { Text("Retry") }
                }
            } else if (buffering && !completed) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                LoadingOverlay(progressPercent = bufferedPercent)
            }
        }
    }
}

@Composable
internal fun LoadingOverlay(progressPercent: Int?, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = (progressPercent ?: 0).coerceIn(0, 100) / 100f,
        label = "Buffered progress"
    )
    val accent = Color(0xFF69E4F4)
    Surface(
        modifier = modifier.width(248.dp),
        color = Color(0xF2141B29),
        contentColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        shadowElevation = 16.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = progressPercent?.let { "Loading ${it.coerceIn(0, 100)}%" } ?: "Loading…",
                style = MaterialTheme.typography.titleMedium
            )
            if (progressPercent == null) {
                LinearProgressIndicator(
                    color = accent,
                    trackColor = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
                        .height(8.dp).clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth().height(8.dp)
                        .clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(progress).height(8.dp).clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(accent, Color(0xFFA6A0FF))))
                    )
                }
            }
            Text(
                text = "Preparing your music",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
