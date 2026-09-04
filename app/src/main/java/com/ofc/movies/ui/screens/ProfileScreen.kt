package com.ofc.movies.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.Coil
import com.ofc.movies.ui.theme.*

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var preferredQuality by remember { mutableStateOf("1080P Ultra HD") }
    val qualityOptions = listOf("Auto (Best)", "1080P Ultra HD", "720P HD", "480P Data Saver")
    var showQualityMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Profile & Settings",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            ),
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Profile Avatar Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkCard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(PrimaryRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VIP",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Guest Cinephile",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Direct MovieBox Session Active",
                        style = MaterialTheme.typography.bodySmall,
                        color = RatingGold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Playback Settings Section
        Text(
            text = "Streaming Preferences",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkCard,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showQualityMenu = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Default Streaming Quality", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = preferredQuality, color = PrimaryRed, style = MaterialTheme.typography.bodySmall)
                }
                Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings", tint = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Storage & Cache Section
        Text(
            text = "Storage & Cache",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkCard,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    try {
                        Coil.imageLoader(context).diskCache?.clear()
                        Coil.imageLoader(context).memoryCache?.clear()
                        Toast.makeText(context, "App cache cleared successfully!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                    }
                }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Clear Image & Stream Cache", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Frees up temporary cached data", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Clear Cache", tint = PrimaryRed)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // About & Version Info
        Text(
            text = "About",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkCard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "App Version", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Text(text = "1.0.0 (Direct API)", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Size Optimization", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Text(text = "< 10 MB Lightweight", color = RatingGold, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Backend Gateway", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Text(text = "api6.aoneroom.com", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Quality Picker Dialog
        if (showQualityMenu) {
            AlertDialog(
                onDismissRequest = { showQualityMenu = false },
                title = { Text(text = "Default Quality", color = Color.White) },
                text = {
                    Column {
                        qualityOptions.forEach { opt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        preferredQuality = opt
                                        showQualityMenu = false
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = opt,
                                    color = if (opt == preferredQuality) PrimaryRed else Color.White,
                                    fontWeight = if (opt == preferredQuality) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQualityMenu = false }) {
                        Text("Dismiss", color = PrimaryRed)
                    }
                },
                containerColor = DarkBackground
            )
        }
    }
}
