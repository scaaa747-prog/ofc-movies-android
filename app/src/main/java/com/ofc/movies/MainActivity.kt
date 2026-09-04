package com.ofc.movies

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ofc.movies.ui.screens.HomeScreen
import com.ofc.movies.ui.theme.DarkBackground
import com.ofc.movies.ui.theme.OFCMoviesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OFCMoviesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    HomeScreen(
                        onMovieClick = { movie ->
                            Toast.makeText(this, "Selected: ${movie.title}", Toast.LENGTH_SHORT).show()
                        },
                        onSearchClick = {
                            Toast.makeText(this, "Search Screen", Toast.LENGTH_SHORT).show()
                        },
                        onProfileClick = {
                            Toast.makeText(this, "Profile Screen", Toast.LENGTH_SHORT).show()
                        },
                        onContinueWatchingClick = { item ->
                            Toast.makeText(this, "Resuming: ${item.title}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}
