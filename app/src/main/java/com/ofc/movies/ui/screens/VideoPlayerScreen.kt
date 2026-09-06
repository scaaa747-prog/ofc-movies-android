package com.ofc.movies.ui.screens

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ofc.movies.data.api.MovieBoxSigner
import com.ofc.movies.data.api.MovieRepository
import com.ofc.movies.data.local.StorageManager
import com.ofc.movies.data.model.DubItem
import com.ofc.movies.data.model.PlayableStream
import com.ofc.movies.data.model.formatUserFriendlyError
import com.ofc.movies.ui.theme.DarkBackground
import com.ofc.movies.ui.theme.DarkCard
import com.ofc.movies.ui.theme.PillShape
import com.ofc.movies.ui.theme.PrimaryRed
import com.ofc.movies.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    movieId: String,
    title: String,
    season: Int = 0,
    episode: Int = 0,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val storageManager = remember { StorageManager.getInstance(context) }
    val repository = remember { MovieRepository(storageManager = storageManager) }

    var streams by remember { mutableStateOf<List<PlayableStream>>(emptyList()) }
    var selectedStream by remember { mutableStateOf<PlayableStream?>(null) }
    var isLoadingStreams by remember { mutableStateOf(true) }
    var streamError by remember { mutableStateOf<String?>(null) }

    var dubs by remember { mutableStateOf<List<DubItem>>(emptyList()) }
    var currentDubSubjectId by remember { mutableStateOf(movieId) }
    var selectedDubName by remember { mutableStateOf("Audio") }
    var showAudioDubDialog by remember { mutableStateOf(false) }

    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var playWhenReady by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var showQualityDialog by remember { mutableStateOf(false) }

    // Fullscreen landscape & hide system bars (immersive mode)
    DisposableEffect(Unit) {
        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())

        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = originalOrientation
        }
    }

    // Fast buffering LoadControl (starts playing after only 1s buffer)
    val loadControl = remember {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                2500,  // minBufferMs
                20000, // maxBufferMs
                1000,  // bufferForPlaybackMs (instant start)
                1500   // bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters())
        }
    }

    val renderersFactory = remember {
        DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
        }
    }

    // Initialize ExoPlayer with optimized loadControl, renderersFactory and trackSelector
    val exoPlayer = remember {
        ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .build().apply {
                this.playWhenReady = true
            }
    }

    // Auto-hide controls after 3.5 seconds of uninterrupted playback
    LaunchedEffect(isControlsVisible, isPlaying, isBuffering) {
        if (isControlsVisible && isPlaying && !isBuffering) {
            delay(3500)
            isControlsVisible = false
        }
    }

    // Periodically update playback position and states
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
            isPlaying = exoPlayer.isPlaying
            isBuffering = (exoPlayer.playbackState == Player.STATE_BUFFERING)
            playWhenReady = exoPlayer.playWhenReady
            delay(400)
        }
    }

    // Fetch movie detail to discover all available dubs
    LaunchedEffect(movieId) {
        val detailRes = repository.getMovieDetail(movieId)
        detailRes.onSuccess { d ->
            if (d.dubs.isNotEmpty()) {
                dubs = d.dubs
                val cur = d.dubs.firstOrNull { it.subjectId == currentDubSubjectId }
                    ?: d.dubs.firstOrNull { it.isOriginal }
                    ?: d.dubs.firstOrNull()
                if (cur != null) {
                    selectedDubName = cur.lanName
                }
            }
        }
    }

    // Load streams (checking offline downloads first, then direct MovieBox API)
    LaunchedEffect(currentDubSubjectId, season, episode) {
        isLoadingStreams = true
        streamError = null

        // 1. Check if user already downloaded this title offline
        val targetId = if (season > 0 && episode > 0) "${movieId}_s${season}_e${episode}" else movieId
        val downloadedItem = storageManager.getDownloads().firstOrNull { it.id == targetId }
            ?: storageManager.getDownloads().firstOrNull { it.id == movieId }
        var localPlaySuccess = false
        if (downloadedItem != null && downloadedItem.status == "Ready") {
            if (downloadedItem.localUri.startsWith("cache://") || downloadedItem.streamUrl.contains(".mpd")) {
                try {
                    val stream = PlayableStream(
                        title = downloadedItem.title,
                        resolution = 720,
                        codecName = "hevc",
                        size = 0L,
                        duration = 0L,
                        streamUrl = downloadedItem.streamUrl,
                        isDash = downloadedItem.streamUrl.contains(".mpd"),
                        signCookie = null,
                        season = season,
                        episode = episode
                    )
                    playStream(context, exoPlayer, stream, isOffline = true)
                    isLoadingStreams = false
                    localPlaySuccess = true
                } catch (e: Exception) {
                    // Fallback to online streams
                }
            } else if (downloadedItem.localUri.isNotEmpty()) {
                try {
                    val localUri = Uri.parse(downloadedItem.localUri)
                    val mediaItem = MediaItem.fromUri(localUri)
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.play()
                    isLoadingStreams = false
                    localPlaySuccess = true
                } catch (e: Exception) {
                    // Fallback
                }
            } else if (downloadedItem.downloadId > 0L) {
                try {
                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                    val localUri = dm?.getUriForDownloadedFile(downloadedItem.downloadId)
                    if (localUri != null) {
                        val mediaItem = MediaItem.fromUri(localUri)
                        exoPlayer.setMediaItem(mediaItem)
                        exoPlayer.prepare()
                        exoPlayer.play()
                        isLoadingStreams = false
                        localPlaySuccess = true
                    }
                } catch (e: Exception) {}
            }
        }

        if (!localPlaySuccess) {
            val result = repository.getPlayableStreams(currentDubSubjectId, season, episode)
            result.onSuccess { sList ->
                streams = sList
                if (sList.isNotEmpty()) {
                    val preferred = storageManager.getDefaultQuality()
                    val stream = sList.firstOrNull { s ->
                        when {
                            preferred.contains("1080") -> s.resolution >= 1080
                            preferred.contains("720") -> s.resolution in 720..1079
                            preferred.contains("480") -> s.resolution < 720
                            else -> true
                        }
                    } ?: sList.first()
                    selectedStream = stream
                    playStream(context, exoPlayer, stream)
                } else {
                    streamError = "No playable stream available for this title."
                }
            }.onFailure { err ->
                streamError = formatUserFriendlyError(err, "Failed to resolve video stream. Please check your internet.")
            }
            isLoadingStreams = false
        }
    }

    // Clean up player on leave & save watch history
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = (state == Player.STATE_BUFFERING)
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayWhenReadyChanged(ready: Boolean, reason: Int) {
                playWhenReady = ready
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val nextStream = streams.firstOrNull { it != selectedStream && it.streamUrl != selectedStream?.streamUrl }
                if (nextStream != null) {
                    selectedStream = nextStream
                    playStream(context, exoPlayer, nextStream)
                } else {
                    streamError = formatUserFriendlyError(error, "Playback error: Unable to play video. Please check your internet.")
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            if (dur > 0 && pos > 2000) {
                storageManager.updateContinueWatching(
                    id = movieId,
                    title = title,
                    coverUrl = "",
                    positionMs = pos,
                    durationMs = dur,
                    season = season,
                    episode = episode
                )
            }
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        isControlsVisible = !isControlsVisible
                    },
                    onDoubleTap = { offset ->
                        // Double tap on left half rewinds 10s, right half forwards 10s
                        if (offset.x < size.width / 2) {
                            val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                        } else {
                            val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration)
                            exoPlayer.seekTo(newPos)
                        }
                    }
                )
            }
    ) {
        // 1. Native Media3 PlayerView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    keepScreenOn = true
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Loading Spinner
        if (isLoadingStreams) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryRed)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Loading direct stream...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // 3. Error overlay
        if (streamError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = streamError ?: "Playback Error", color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                        shape = PillShape
                    ) {
                        Text("Go Back", color = Color.White)
                    }
                }
            }
        }

        // 4. Custom Controls Overlay
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        if (season > 0 && episode > 0) {
                            Text(
                                text = "Season $season • Episode $episode",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Audio Dub Selector Pill
                    if (dubs.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DarkCard,
                            modifier = Modifier.clickable { showAudioDubDialog = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Language,
                                    contentDescription = "Audio Dub",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedDubName,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    // Stream Quality Pill (e.g. 1080P)
                    selectedStream?.let { st ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = PrimaryRed,
                            modifier = Modifier.clickable { showQualityDialog = true }
                        ) {
                            Text(
                                text = st.title,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Center Playback Controls (-10s, Play/Pause, +10s)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(40.dp)
                ) {
                    // Rewind 10s
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Play / Pause / Buffering Circle Button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(PrimaryRed)
                            .clickable {
                                if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                    exoPlayer.seekTo(0L)
                                    exoPlayer.play()
                                } else {
                                    if (playWhenReady) {
                                        exoPlayer.pause()
                                    } else {
                                        exoPlayer.play()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying || playWhenReady) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying || playWhenReady) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    // Forward 10s
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration)
                            exoPlayer.seekTo(newPos)
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom Bar: Scrubber + Timing
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Slider(
                        value = if (totalDurationMs > 0) currentPositionMs.toFloat() / totalDurationMs.toFloat() else 0f,
                        onValueChange = { frac ->
                            val target = (frac * totalDurationMs).toLong()
                            exoPlayer.seekTo(target)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryRed,
                            activeTrackColor = PrimaryRed,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDuration(currentPositionMs),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = formatDuration(totalDurationMs),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Quality Selection Dialog
        if (showQualityDialog && streams.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { showQualityDialog = false },
                title = { Text(text = "Select Stream Quality", color = Color.White) },
                text = {
                    Column {
                        streams.forEach { st ->
                            val isSelected = st == selectedStream
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedStream = st
                                        showQualityDialog = false
                                        val currentPos = exoPlayer.currentPosition

                                        if (st.isDash) {
                                            val maxH = st.resolution
                                            val minH = when {
                                                maxH >= 1080 -> 1080
                                                maxH >= 720 -> 720
                                                else -> 0
                                            }
                                            trackSelector.setParameters(
                                                trackSelector.buildUponParameters()
                                                    .setMaxVideoSize(maxH * 16 / 9, maxH)
                                                    .setMinVideoSize(minH * 16 / 9, minH)
                                            )
                                            val currentUri = exoPlayer.currentMediaItem?.localConfiguration?.uri?.toString()
                                            if (currentUri != st.streamUrl) {
                                                playStream(context, exoPlayer, st, currentPos)
                                            }
                                        } else {
                                            playStream(context, exoPlayer, st, currentPos)
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${st.title} (${st.codecName.uppercase()})",
                                    color = if (isSelected) PrimaryRed else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = PrimaryRed
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQualityDialog = false }) {
                        Text("Close", color = PrimaryRed)
                    }
                },
                containerColor = DarkBackground
            )
        }

        // Audio Dub Selection Dialog
        if (showAudioDubDialog && dubs.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { showAudioDubDialog = false },
                title = { Text(text = "Select Audio Dub", color = Color.White) },
                text = {
                    Column {
                        dubs.forEach { dub ->
                            val isSelected = dub.subjectId == currentDubSubjectId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (dub.subjectId != currentDubSubjectId) {
                                            val resumePos = exoPlayer.currentPosition
                                            currentDubSubjectId = dub.subjectId
                                            selectedDubName = dub.lanName
                                            showAudioDubDialog = false
                                            isLoadingStreams = true
                                            scope.launch {
                                                val result = repository.getPlayableStreams(dub.subjectId, season, episode)
                                                result.onSuccess { sList ->
                                                    streams = sList
                                                    val st = sList.firstOrNull { it.resolution == (selectedStream?.resolution ?: 1080) }
                                                        ?: sList.firstOrNull()
                                                    if (st != null) {
                                                        selectedStream = st
                                                        playStream(context, exoPlayer, st, resumePos)
                                                    }
                                                }
                                                isLoadingStreams = false
                                            }
                                        } else {
                                            showAudioDubDialog = false
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dub.lanName + if (dub.isOriginal) " (Original)" else "",
                                    color = if (isSelected) PrimaryRed else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = PrimaryRed
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAudioDubDialog = false }) {
                        Text("Close", color = PrimaryRed)
                    }
                },
                containerColor = DarkBackground
            )
        }
    }
}

@OptIn(UnstableApi::class)
private fun playStream(
    context: Context,
    player: ExoPlayer,
    stream: PlayableStream,
    seekToMs: Long = 0L,
    isOffline: Boolean = false
) {
    player.stop()
    player.clearMediaItems()

    val dataSourceFactory = if (isOffline) {
        com.ofc.movies.data.download.DownloadCacheManager.createReadOnlyCacheDataSourceFactory(
            context.applicationContext,
            stream.signCookie
        )
    } else {
        com.ofc.movies.data.download.DownloadCacheManager.createHttpDataSourceFactory(
            stream.signCookie
        )
    }

    val mediaItem = MediaItem.Builder()
        .setUri(stream.streamUrl)
        .apply {
            if (stream.isDash) {
                setMimeType(MimeTypes.APPLICATION_MPD)
            }
        }
        .build()

    val mediaSource = if (stream.isDash) {
        DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
    } else {
        ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
    }

    player.setMediaSource(mediaSource)
    player.prepare()
    if (seekToMs > 0L) {
        player.seekTo(seekToMs)
    }
    player.play()
}

private fun formatDuration(ms: Long): String {
    val totalSecs = (ms / 1000).coerceAtLeast(0)
    val hrs = totalSecs / 3600
    val mins = (totalSecs % 3600) / 60
    val secs = totalSecs % 60
    return if (hrs > 0) {
        "%d:%02d:%02d".format(hrs, mins, secs)
    } else {
        "%02d:%02d".format(mins, secs)
    }
}
