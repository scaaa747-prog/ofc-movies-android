package com.ofc.movies.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ofc.movies.ui.theme.*

// Self-contained download icon vector (0 external dependencies)
val DownloadNavIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "DownloadNavIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(19f, 9f)
            horizontalLineTo(15f)
            verticalLineTo(3f)
            horizontalLineTo(9f)
            verticalLineTo(9f)
            horizontalLineTo(5f)
            lineTo(12f, 16f)
            lineTo(19f, 9f)
            close()
            moveTo(5f, 18f)
            verticalLineTo(20f)
            horizontalLineTo(19f)
            verticalLineTo(18f)
            horizontalLineTo(5f)
            close()
        }
    }.build()
}

enum class NavTab(
    val title: String,
    val icon: ImageVector
) {
    HOME("Home", Icons.Filled.Home),
    SEARCH("Search", Icons.Filled.Search),
    DOWNLOADS("Downloads", DownloadNavIcon),
    PROFILE("Profile", Icons.Filled.Person)
}

@Composable
fun BottomNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = DarkBackground.copy(alpha = 0.96f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Subtle top divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DarkBorder)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTab.values().forEach { tab ->
                    val isSelected = tab == selectedTab
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) PrimaryRed else TextSecondary,
                        animationSpec = tween(durationMillis = 200),
                        label = "iconColor"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) TextPrimary else TextSecondary,
                        animationSpec = tween(durationMillis = 200),
                        label = "textColor"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onTabSelected(tab)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Pill indicator above active tab
                        Box(
                            modifier = Modifier
                                .width(if (isSelected) 18.dp else 0.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(if (isSelected) PrimaryRed else Color.Transparent)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = tab.title,
                            color = textColor,
                            style = NavLabelStyle,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
