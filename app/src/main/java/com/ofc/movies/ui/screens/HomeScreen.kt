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
    viewModel: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(NavTab.HOME) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    selectedTab = tab
                    when (tab) {
                        NavTab.SEARCH -> onSearchClick()
                        NavTab.PROFILE -> onProfileClick()
                        else -> { /* Stay or navigate */ }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
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
                            contentPadding = PaddingValues(bottom = 24.dp)
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

                            // 2. Continue Watching Section (with progress bar overlay)
                            if (state.continueWatching.isNotEmpty()) {
                                item(key = "continue_watching_section") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp, bottom = 8.dp)
                                    ) {
                                        Text(
                                            text = "Continue Watching",
                                            style = MaterialTheme.typography.titleMedium,
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

                            // 3. Horizontal Scrolling Content Rows (Trending, New Releases, Genres)
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
                // Search Icon Button
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

                // Profile Avatar Icon (Circular red border)
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
        // Hero Banner Skeleton
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
            shape = RoundedCornerShape(0.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Row 1 Skeleton
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            ShimmerBox(
                modifier = Modifier
                    .width(140.dp)
                    .height(20.dp),
                shape = RoundedCornerShape(4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) {
                    ShimmerBox(
                        modifier = Modifier
                            .width(120.dp)
                            .aspectRatio(2f / 3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
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
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Unable to load movies",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NetflixRed)
            ) {
                Text(text = "Retry", color = TextPrimary)
            }
        }
    }
}
