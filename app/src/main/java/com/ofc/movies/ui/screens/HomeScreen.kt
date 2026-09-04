package com.ofc.movies.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ofc.movies.data.model.ContinueWatchingItem
import com.ofc.movies.data.model.MovieItem
import com.ofc.movies.ui.components.*
import com.ofc.movies.ui.theme.*

@Composable
fun HomeScreen(
    onMovieClick: (MovieItem) -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onContinueWatchingClick: (ContinueWatchingItem) -> Unit,
    onCategoryClick: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Action", "Drama", "Sci-Fi", "Comedy", "Animation", "Thriller")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Crossfade(
            targetState = uiState,
            animationSpec = tween(400),
            label = "homeCrossfade"
        ) { state ->
            when (state) {
                is HomeUiState.Loading -> {
                    HomeScreenSkeleton()
                }
                is HomeUiState.Error -> {
                    HomeErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadHomeContent() }
                    )
                }
                is HomeUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // 1. Hero Banner Carousel
                        if (state.heroMovies.isNotEmpty()) {
                            item(key = "hero_banner") {
                                HeroBanner(
                                    featuredMovies = state.heroMovies,
                                    onPlayClick = { movie -> onMovieClick(movie) },
                                    onDetailClick = { movie -> onMovieClick(movie) }
                                )
                            }
                        }

                        // 2. Category Filter Pills
                        item(key = "category_chips") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(categories) { cat ->
                                    val isSelected = cat == selectedCategory
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = if (isSelected) PrimaryRed else DarkCard,
                                        modifier = Modifier.clickable {
                                            selectedCategory = cat
                                            if (cat != "All") {
                                                onCategoryClick(cat)
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = cat,
                                            color = if (isSelected) Color.White else TextSecondary,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Continue Watching Section (with progress bar overlay)
                        if (state.continueWatching.isNotEmpty()) {
                            item(key = "continue_watching_section") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 8.dp)
                                ) {
                                    Text(
                                        text = "Continue Watching",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.5).sp
                                        ),
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(state.continueWatching, key = { it.id }) { item ->
                                            ContinueWatchingCard(
                                                item = item,
                                                onClick = { onContinueWatchingClick(item) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Top 10 with big numbers
                        if (state.top10Movies.isNotEmpty()) {
                            item(key = "top_10_today") {
                                Top10MovieRow(
                                    movies = state.top10Movies,
                                    onMovieClick = onMovieClick
                                )
                            }
                        }

                        // 5. Horizontal Scrolling Content Rows (Hollywood, Trending, Coming Soon, etc.)
                        items(state.rows, key = { it.title }) { row ->
                            Spacer(modifier = Modifier.height(12.dp))
                            MovieRow(
                                title = row.title,
                                movies = row.items,
                                onMovieClick = onMovieClick
                            )
                        }
                    }
                }
            }
        }

        // Top Header: App Logo + Search Icon + Profile Avatar (Floating on gradient)
        HomeTopBar(
            onSearchClick = onSearchClick,
            onProfileClick = onProfileClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun HomeTopBar(
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground.copy(alpha = 0.95f),
                        DarkBackground.copy(alpha = 0.65f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Brand Logo (Netflix Red)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NetflixRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "OFC MOVIES",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = TextPrimary
                )
            }

            // Action Icons (Search + Profile Avatar)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceElevated)
                        .clickable(onClick = onProfileClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "U",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = NetflixRed
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreenSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Hero Skeleton
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Row 1 Title Skeleton
        ShimmerBox(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(width = 140.dp, height = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Row 1 Posters Skeleton
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(4) {
                ShimmerBox(
                    modifier = Modifier
                        .size(width = 120.dp, height = 180.dp)
                        .clip(PosterShape)
                )
            }
        }
    }
}

@Composable
fun HomeErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Unable to connect",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetry,
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "Retry",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
