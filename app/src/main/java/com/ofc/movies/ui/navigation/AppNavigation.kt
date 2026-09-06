package com.ofc.movies.ui.navigation

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ofc.movies.ui.components.BottomNavBar
import com.ofc.movies.ui.components.NavTab
import com.ofc.movies.ui.screens.*
import com.ofc.movies.ui.theme.DarkBackground

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val MAIN = "main?tab={tab}"
    const val DETAIL = "detail/{movieId}"
    const val PLAYER = "player/{movieId}/{title}/{se}/{ep}"
    const val CATEGORY = "category/{categoryName}"

    fun main(tab: String? = null): String = if (tab != null) "main?tab=$tab" else "main"
    fun detail(movieId: String): String = "detail/$movieId"
    fun player(movieId: String, title: String, se: Int = 0, ep: Int = 0): String {
        val encTitle = Uri.encode(title)
        return "player/$movieId/$encTitle/$se/$ep"
    }
    fun category(categoryName: String): String {
        val enc = Uri.encode(categoryName)
        return "category/$enc"
    }
}

@Composable
fun AppNavigation(initialTab: String? = null) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onSplashFinished = { isOnboardingCompleted ->
                    val destination = if (isOnboardingCompleted) Routes.main(initialTab) else Routes.ONBOARDING
                    navController.navigate(destination) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.main(initialTab)) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.MAIN,
            arguments = listOf(
                navArgument("tab") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val tab = backStackEntry.arguments?.getString("tab") ?: initialTab
            MainContainerScreen(
                initialTabName = tab,
                onMovieClick = { movie ->
                    navController.navigate(Routes.detail(movie.id))
                },
                onCategoryClick = { categoryName ->
                    navController.navigate(Routes.category(categoryName))
                },
                onPlayOffline = { movieId, title ->
                    navController.navigate(Routes.player(movieId, title, 0, 0))
                }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(
                navArgument("movieId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
            MovieDetailScreen(
                movieId = movieId,
                onBackClick = { navController.popBackStack() },
                onPlayClick = { id, title, se, ep ->
                    navController.navigate(Routes.player(id, title, se, ep))
                },
                onRelatedMovieClick = { relatedMovie ->
                    navController.navigate(Routes.detail(relatedMovie.id))
                },
                onGoToDownloads = {
                    navController.navigate(Routes.main("downloads")) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Routes.PLAYER,
            arguments = listOf(
                navArgument("movieId") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("se") { type = NavType.IntType; defaultValue = 0 },
                navArgument("ep") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
            val rawTitle = backStackEntry.arguments?.getString("title") ?: "Now Playing"
            val title = Uri.decode(rawTitle)
            val se = backStackEntry.arguments?.getInt("se") ?: 0
            val ep = backStackEntry.arguments?.getInt("ep") ?: 0

            VideoPlayerScreen(
                movieId = movieId,
                title = title,
                season = se,
                episode = ep,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CATEGORY,
            arguments = listOf(
                navArgument("categoryName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rawCat = backStackEntry.arguments?.getString("categoryName") ?: ""
            val categoryName = Uri.decode(rawCat)
            CategoryScreen(
                categoryName = categoryName,
                onBackClick = { navController.popBackStack() },
                onMovieClick = { movie ->
                    navController.navigate(Routes.detail(movie.id))
                }
            )
        }
    }
}

@Composable
fun MainContainerScreen(
    initialTabName: String? = null,
    onMovieClick: (com.ofc.movies.data.model.MovieItem) -> Unit,
    onCategoryClick: (String) -> Unit,
    onPlayOffline: (movieId: String, title: String) -> Unit
) {
    val initialNavTab = when (initialTabName?.lowercase()) {
        "downloads" -> NavTab.DOWNLOADS
        "search" -> NavTab.SEARCH
        "mylist" -> NavTab.MY_LIST
        "profile" -> NavTab.PROFILE
        else -> NavTab.HOME
    }
    var selectedTab by remember(initialTabName) { mutableStateOf(initialNavTab) }

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .background(DarkBackground)
        ) {
            Crossfade(
                targetState = selectedTab,
                label = "tabCrossfade"
            ) { tab ->
                when (tab) {
                    NavTab.HOME -> {
                        HomeScreen(
                            onMovieClick = onMovieClick,
                            onSearchClick = { selectedTab = NavTab.SEARCH },
                            onProfileClick = { selectedTab = NavTab.PROFILE },
                            onContinueWatchingClick = { item ->
                                onPlayOffline(item.id, item.title)
                            },
                            onCategoryClick = onCategoryClick
                        )
                    }
                    NavTab.SEARCH -> {
                        SearchScreen(
                            onMovieClick = onMovieClick
                        )
                    }
                    NavTab.MY_LIST -> {
                        MyListScreen(
                            onMovieClick = onMovieClick
                        )
                    }
                    NavTab.DOWNLOADS -> {
                        DownloadsScreen(
                            onPlayOffline = onPlayOffline,
                            onBrowseMovies = { selectedTab = NavTab.HOME }
                        )
                    }
                    NavTab.PROFILE -> {
                        ProfileScreen()
                    }
                }
            }
        }
    }
}
