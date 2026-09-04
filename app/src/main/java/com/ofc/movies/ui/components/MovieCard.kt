package com.ofc.movies.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.ofc.movies.data.api.ApiClient
import com.ofc.movies.data.model.MovieItem
import com.ofc.movies.ui.theme.*

@Composable
fun MovieCard(
    movie: MovieItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth subtle micro-interaction scale effect on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "cardScale"
    )

    val fullCoverUrl = remember(movie.coverUrl) {
        ApiClient.getAbsoluteUrl(movie.coverUrl)
    }

    val widthMod = if (width != null) Modifier.width(width) else Modifier.fillMaxWidth()

    Column(
        modifier = modifier
            .then(widthMod)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Poster Card (2:3 Aspect Ratio, 8dp corner radius, subtle shadow)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .shadow(elevation = 6.dp, shape = PosterShape, clip = false)
                .clip(PosterShape)
                .background(DarkSurface)
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(fullCoverUrl)
                    .crossfade(300)
                    .build(),
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    ShimmerBox(modifier = Modifier.fillMaxSize(), shape = PosterShape)
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = movie.title.take(1),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextMuted
                        )
                    }
                }
            )

            // Bottom subtle gradient on poster
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, DarkBackground.copy(alpha = 0.8f))
                        )
                    )
            )

            // Rating Badge (Gold) top-left
            if (!movie.rating.isNullOrBlank() && movie.rating != "0") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(
                            color = DarkBackground.copy(alpha = 0.82f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Rating",
                        tint = RatingGold,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = movie.rating,
                        style = MaterialTheme.typography.labelSmall,
                        color = RatingGold,
                        fontSize = 10.sp
                    )
                }
            }

            // Top-right Dub / Corner Badge (e.g. "Hindi", "4K")
            val corner = movie.cornerText
            if (!corner.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(
                            color = PrimaryRed.copy(alpha = 0.90f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = corner,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title text (Bold, condensed typography, max 1 line with ellipsis)
        Text(
            text = movie.title,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Subtitle: Year & Genre
        val year = movie.displayYear
        val genre = movie.genre?.split(",")?.firstOrNull()?.trim()
        val metadataText = listOfNotNull(
            year.takeIf { it.isNotBlank() },
            genre.takeIf { !it.isNullOrBlank() }
        ).joinToString(" • ")

        if (metadataText.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = metadataText,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
