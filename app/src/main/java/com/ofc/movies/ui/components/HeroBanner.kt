package com.ofc.movies.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.ofc.movies.data.api.ApiClient
import com.ofc.movies.data.model.MovieItem
import com.ofc.movies.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun HeroBanner(
    featuredMovies: List<MovieItem>,
    onPlayClick: (MovieItem) -> Unit,
    onDetailClick: (MovieItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (featuredMovies.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }

    // Auto-scrolling carousel (cycles every 5 seconds)
    LaunchedEffect(featuredMovies) {
        while (true) {
            delay(5000)
            if (featuredMovies.isNotEmpty()) {
                currentIndex = (currentIndex + 1) % featuredMovies.size
            }
        }
    }

    val currentMovie = featuredMovies.getOrNull(currentIndex) ?: featuredMovies.first()
    val fullCoverUrl = remember(currentMovie.coverUrl) {
        ApiClient.getThumbnailUrl(currentMovie.coverUrl, width = 720)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(440.dp)
            .background(DarkBackground)
    ) {
        // Full-width Hero Poster
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(fullCoverUrl)
                .crossfade(600)
                .build(),
            contentDescription = currentMovie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) },
            error = { Box(modifier = Modifier.fillMaxSize().background(DarkSurface)) }
        )

        // Cinematic Gradient Overlays (Top subtle dark, bottom deep fade to #0A0A0F)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .background(TopBarGradient)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HeroGradient)
        )

        // Overlay Content at Bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Badges Row (Dub / Audio Language, Rating, Genre)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                val audioLang = currentMovie.audioLanguage
                if (!audioLang.isNullOrBlank()) {
                    Text(
                        text = audioLang,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        modifier = Modifier
                            .background(NetflixRed, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (!currentMovie.rating.isNullOrBlank() && currentMovie.rating != "0") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Rating",
                            tint = GoldAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${currentMovie.rating} IMDb",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldAccent
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (!currentMovie.genre.isNullOrBlank()) {
                    Text(
                        text = currentMovie.genre.split(",").firstOrNull()?.trim() ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            // Cinematic Headline Title (Condensed, tight letter spacing, bold)
            Text(
                text = currentMovie.displayTitle,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // CTA Buttons Row (Pill-shaped 24dp radius)
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary Play Button (#E50914, Pill-shaped)
                Button(
                    onClick = { onPlayClick(currentMovie) },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NetflixRed,
                        contentColor = TextPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 11.dp),
                    modifier = Modifier.shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Play",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        fontSize = 15.sp
                    )
                }

                // Info / Details Button (Dark Surface, Pill-shaped)
                FilledTonalButton(
                    onClick = { onDetailClick(currentMovie) },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = DarkSurfaceElevated.copy(alpha = 0.9f),
                        contentColor = TextPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Info",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Details",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Carousel Indicators
            if (featuredMovies.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    featuredMovies.take(6).forEachIndexed { index, _ ->
                        val isSelected = index == currentIndex
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 6.dp else 4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) NetflixRed else TextMuted.copy(alpha = 0.5f))
                                .clickable { currentIndex = index }
                        )
                    }
                }
            }
        }
    }
}
