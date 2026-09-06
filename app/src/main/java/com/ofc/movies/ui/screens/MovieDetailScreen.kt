package com.ofc.movies.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ofc.movies.data.api.ApiClient
import com.ofc.movies.data.api.MovieBoxSigner
import com.ofc.movies.data.api.MovieRepository
import com.ofc.movies.data.local.DownloadedItem
import com.ofc.movies.data.local.StorageManager
import com.ofc.movies.data.model.*
import com.ofc.movies.ui.components.MovieCard
import com.ofc.movies.ui.components.ShimmerBox
import com.ofc.movies.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MovieDetailScreen(
    movieId: String,
    onBackClick: () -> Unit,
    onPlayClick: (movieId: String, title: String, se: Int, ep: Int) -> Unit,
    onRelatedMovieClick: (MovieItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storageManager = remember { StorageManager.getInstance(context) }
    val repository = remember { MovieRepository(storageManager = storageManager) }
    val scope = rememberCoroutineScope()

    var movieDetail by remember { mutableStateOf<MovieDetailData?>(null) }
    var seasons by remember { mutableStateOf<List<SeasonItem>>(emptyList()) }
    var recommendations by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var selectedDubId by remember { mutableStateOf(movieId) }
    var selectedSeason by remember { mutableIntStateOf(1) }
    var selectedEpisode by remember { mutableIntStateOf(1) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var isInMyList by remember { mutableStateOf(storageManager.isInWatchlist(movieId)) }
    var isMovieDownloaded by remember { mutableStateOf(storageManager.isDownloaded(movieId)) }

    LaunchedEffect(movieId) {
        isLoading = true
        errorMsg = null
        selectedDubId = movieId
        isInMyList = storageManager.isInWatchlist(movieId)
        isMovieDownloaded = storageManager.isDownloaded(movieId)

        val detResult = repository.getMovieDetail(movieId)
        detResult.onSuccess { data ->
            movieDetail = data
            // Fetch seasons ONLY if TV Series (subjectType == 2)
            if (data.subjectType == 2) {
                val seasonsRes = repository.getSeasonInfo(movieId)
                seasonsRes.onSuccess { sList ->
                    val validSeasons = sList.filter { it.seasonNumber > 0 }
                    seasons = validSeasons
                    if (validSeasons.isNotEmpty()) {
                        selectedSeason = validSeasons.first().seasonNumber
                        selectedEpisode = 1
                    }
                }
            } else {
                seasons = emptyList()
                selectedSeason = 0
                selectedEpisode = 0
            }

            // Fetch recommendations
            val recResult = repository.getRecommendations(movieId)
            recResult.onSuccess { rList ->
                recommendations = rList
            }
        }.onFailure { err ->
            errorMsg = err.localizedMessage ?: "Failed to load movie details"
        }
        isLoading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryRed)
            }
        } else if (errorMsg != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = errorMsg ?: "Error loading title", color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMsg = null
                                val detResult = repository.getMovieDetail(movieId)
                                detResult.onSuccess { data ->
                                    movieDetail = data
                                    if (data.subjectType == 2) {
                                        val seasonsRes = repository.getSeasonInfo(movieId)
                                        seasonsRes.onSuccess { sList ->
                                            val validSeasons = sList.filter { it.seasonNumber > 0 }
                                            seasons = validSeasons
                                            if (validSeasons.isNotEmpty()) {
                                                selectedSeason = validSeasons.first().seasonNumber
                                                selectedEpisode = 1
                                            }
                                        }
                                    } else {
                                        seasons = emptyList()
                                        selectedSeason = 0
                                        selectedEpisode = 0
                                    }
                                    val recResult = repository.getRecommendations(movieId)
                                    recResult.onSuccess { recommendations = it }
                                }.onFailure { err ->
                                    errorMsg = err.localizedMessage ?: "Failed to load movie details"
                                }
                                isLoading = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                        shape = PillShape
                    ) {
                        Text("Retry", color = Color.White)
                    }
                }
            }
        } else {
            val detail = movieDetail ?: return@Box
            val context = LocalContext.current
            val coverUrl = ApiClient.getAbsoluteUrl(detail.coverUrl)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                // 1. Parallax Backdrop Header
                item(key = "header_backdrop") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(coverUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = detail.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top Gradient for Status Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(DarkBackground.copy(alpha = 0.8f), Color.Transparent)
                                    )
                                )
                        )

                        // Bottom Gradient Fade to DarkBackground
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            DarkBackground.copy(alpha = 0.7f),
                                            DarkBackground
                                        )
                                    )
                                )
                        )
                    }
                }

                // 2. Title, Badges, and Metadata
                item(key = "metadata") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Text(
                            text = detail.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Gold Rating Badge
                            detail.rating?.let { r ->
                                if (r.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = RatingGold.copy(alpha = 0.15f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Star,
                                                contentDescription = "Rating",
                                                tint = RatingGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = r,
                                                color = RatingGold,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }

                            // Release Year
                            detail.releaseDate?.let { date ->
                                if (date.length >= 4) {
                                    Text(
                                        text = date.take(4),
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            // Quality Badge
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = DarkSurfaceElevated
                            ) {
                                Text(
                                    text = "4K ULTRA HD",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Genres
                        detail.genre?.let { g ->
                            if (g.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = g,
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 3. Primary Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play Button (Large Red Pill)
                            Button(
                                onClick = {
                                    val playSeason = if (detail.subjectType == 2 && seasons.isNotEmpty()) selectedSeason else 0
                                    val playEpisode = if (detail.subjectType == 2 && seasons.isNotEmpty()) selectedEpisode else 0
                                    storageManager.updateContinueWatching(
                                        id = movieId,
                                        title = detail.title,
                                        coverUrl = detail.coverUrl,
                                        positionMs = 1000L,
                                        durationMs = 7200000L,
                                        season = playSeason,
                                        episode = playEpisode
                                    )
                                    onPlayClick(
                                        selectedDubId,
                                        detail.title,
                                        playSeason,
                                        playEpisode
                                    )
                                },
                                shape = PillShape,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Play Now",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            // Download Button (Real offline manager via Android DownloadManager)
                            IconButton(
                                onClick = {
                                    if (isMovieDownloaded) {
                                        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                                        val existing = storageManager.getDownloads().firstOrNull { it.id == movieId }
                                        if (existing != null && existing.downloadId > 0L) {
                                            try { dm?.remove(existing.downloadId) } catch (e: Exception) {}
                                        }
                                        storageManager.removeDownload(movieId)
                                        isMovieDownloaded = false
                                        Toast.makeText(context, "Removed from Downloads", Toast.LENGTH_SHORT).show()
                                    } else {
                                        scope.launch {
                                            Toast.makeText(context, "Preparing download...", Toast.LENGTH_SHORT).show()
                                            val playSeason = if (detail.subjectType == 2 && seasons.isNotEmpty()) selectedSeason else 0
                                            val playEpisode = if (detail.subjectType == 2 && seasons.isNotEmpty()) selectedEpisode else 0
                                            val stream = repository.getDownloadStream(selectedDubId, playSeason, playEpisode)
                                            if (stream != null && stream.streamUrl.isNotEmpty() && !MovieBoxSigner.isFakeClipUrl(stream.streamUrl)) {
                                                try {
                                                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                                    val cleanTitle = detail.title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                                                    val fileName = "${cleanTitle}_${stream.resolution}p.mp4"
                                                    val req = DownloadManager.Request(Uri.parse(stream.streamUrl))
                                                        .setTitle(detail.title)
                                                        .setDescription("Downloading ${stream.title}")
                                                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                        .setAllowedOverMetered(true)
                                                        .setAllowedOverRoaming(true)

                                                    val cleanCookie = stream.signCookie?.trim()?.trimEnd(';') ?: ""
                                                    if (cleanCookie.isNotEmpty()) {
                                                        req.addRequestHeader("Cookie", cleanCookie)
                                                        req.addRequestHeader("Referer", "https://www.movieboxpro.app/")
                                                    }
                                                    req.addRequestHeader("User-Agent", MovieBoxSigner.ANDROID_USER_AGENT)

                                                    try {
                                                        req.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                                                    } catch (e: Exception) {
                                                        try {
                                                            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                                                        } catch (e2: Exception) {}
                                                    }

                                                    val downloadId = dm.enqueue(req)

                                                    storageManager.addDownload(
                                                        DownloadedItem(
                                                            id = movieId,
                                                            title = detail.title,
                                                            coverUrl = detail.coverUrl,
                                                            sizeText = if (stream.size > 0) "${stream.size / (1024 * 1024)} MB" else "${stream.resolution}P",
                                                            quality = stream.title,
                                                            downloadTimeMs = System.currentTimeMillis(),
                                                            streamUrl = stream.streamUrl,
                                                            downloadId = downloadId
                                                        )
                                                    )
                                                    isMovieDownloaded = true
                                                    Toast.makeText(context, "Download started! Check notifications", Toast.LENGTH_LONG).show()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "Direct download is not available for this title. Online streaming is active!", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(DarkCard)
                            ) {
                                Icon(
                                    imageVector = if (isMovieDownloaded) Icons.Filled.Check else Icons.Filled.ArrowDownward,
                                    contentDescription = "Download",
                                    tint = if (isMovieDownloaded) RatingGold else Color.White
                                )
                            }

                            // My List Button (Real persistent watchlist)
                            IconButton(
                                onClick = {
                                    val item = MovieItem(
                                        id = movieId,
                                        title = detail.title,
                                        rating = detail.rating,
                                        year = detail.releaseDate,
                                        genre = detail.genre,
                                        description = detail.description,
                                        rawCover = detail.coverUrl
                                    )
                                    val added = storageManager.toggleWatchlist(item)
                                    isInMyList = added
                                    val msg = if (added) "Saved to My List" else "Removed from My List"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(DarkCard)
                            ) {
                                Icon(
                                    imageVector = if (isInMyList) Icons.Filled.Check else Icons.Filled.Add,
                                    contentDescription = "My List",
                                    tint = if (isInMyList) PrimaryRed else Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 4. Dubs / Language Selector (if available)
                        if (detail.dubs.isNotEmpty()) {
                            Text(
                                text = "Audio Dubs",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(detail.dubs) { dub ->
                                    val isSelected = dub.subjectId == selectedDubId
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = if (isSelected) PrimaryRed else DarkCard,
                                        modifier = Modifier.clickable { selectedDubId = dub.subjectId }
                                    ) {
                                        Text(
                                            text = dub.lanName,
                                            color = if (isSelected) Color.White else TextSecondary,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // 5. Seasons & Episodes Selector (for Series)
                        if (seasons.isNotEmpty()) {
                            Text(
                                text = "Seasons & Episodes",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Seasons Row
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(seasons) { s ->
                                    val isSelected = s.seasonNumber == selectedSeason
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = if (isSelected) PrimaryRed else DarkCard,
                                        modifier = Modifier.clickable {
                                            selectedSeason = s.seasonNumber
                                            selectedEpisode = 1
                                        }
                                    ) {
                                        Text(
                                            text = "Season ${s.seasonNumber}",
                                            color = if (isSelected) Color.White else TextSecondary,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Episodes Row
                            val currentSeason = seasons.firstOrNull { it.seasonNumber == selectedSeason } ?: seasons.first()
                            val maxEp = currentSeason.maxEpisode
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items((1..maxEp).toList()) { epNum ->
                                    val isSelected = epNum == selectedEpisode
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) PrimaryRed.copy(alpha = 0.25f) else DarkCard,
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, PrimaryRed) else null,
                                        modifier = Modifier.clickable { selectedEpisode = epNum }
                                    ) {
                                        Text(
                                            text = "EP $epNum",
                                            color = if (isSelected) PrimaryRed else TextPrimary,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // 6. Storyline Description (with expand / collapse)
                        detail.description?.let { desc ->
                            if (desc.isNotEmpty()) {
                                Text(
                                    text = "Storyline",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    lineHeight = 22.sp,
                                    maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .animateContentSize()
                                        .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                                )
                                Text(
                                    text = if (isDescriptionExpanded) "Show Less" else "Read More",
                                    color = PrimaryRed,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }

                        // 7. Cast List
                        if (detail.cast.isNotEmpty()) {
                            Text(
                                text = "Cast & Crew",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(detail.cast.take(10)) { person ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(72.dp)
                                    ) {
                                        val avatar = ApiClient.getAbsoluteUrl(person.avatarUrl ?: "")
                                        if (avatar.isNotEmpty()) {
                                            AsyncImage(
                                                model = avatar,
                                                contentDescription = person.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(CircleShape)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(CircleShape)
                                                    .background(DarkSurfaceElevated),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = person.name.take(1),
                                                    color = PrimaryRed,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = person.name,
                                            color = TextPrimary,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        person.role?.let { r ->
                                            Text(
                                                text = r,
                                                color = TextSecondary,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // 8. Recommendations ("More Like This")
                        if (recommendations.isNotEmpty()) {
                            Text(
                                text = "More Like This",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(recommendations) { recMovie ->
                                    MovieCard(
                                        movie = recMovie,
                                        onClick = { onRelatedMovieClick(recMovie) },
                                        width = 110.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Back Button (Top Left)
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(DarkBackground.copy(alpha = 0.7f))
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
