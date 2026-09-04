package com.ofc.movies.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ofc.movies.data.model.MovieItem
import com.ofc.movies.ui.theme.PrimaryRed
import com.ofc.movies.ui.theme.TextPrimary

@Composable
fun Top10MovieRow(
    movies: List<MovieItem>,
    onMovieClick: (MovieItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (movies.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Top 10 Today",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            ),
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(movies.take(10), key = { _, item -> item.id }) { index, movie ->
                Box(
                    modifier = Modifier.height(180.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    // Big Number background (e.g. 1 to 10)
                    Text(
                        text = "${index + 1}",
                        style = TextStyle(
                            fontSize = 90.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = PrimaryRed.copy(alpha = 0.85f)
                        ),
                        modifier = Modifier
                            .offset(x = (-8).dp, y = 14.dp)
                            .align(Alignment.BottomStart)
                    )

                    // Movie poster shifted right
                    Box(modifier = Modifier.padding(start = 38.dp)) {
                        MovieCard(
                            movie = movie,
                            onClick = { onMovieClick(movie) },
                            width = 110.dp
                        )
                    }
                }
            }
        }
    }
}
