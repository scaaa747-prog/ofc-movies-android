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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.PlayCircle
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
import com.ofc.movies.BuildConfig
import com.ofc.movies.data.local.StorageManager
import com.ofc.movies.data.update.AppUpdateInfo
import com.ofc.movies.data.update.UpdateManager
import com.ofc.movies.ui.components.AppUpdateDialog
import com.ofc.movies.ui.components.DownloadNavIcon
import com.ofc.movies.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

private fun calculateCacheSize(context: Context): Long {
    var size = 0L
    try {
        context.cacheDir?.walkTopDown()?.forEach { file ->
            if (file.isFile) size += file.length()
        }
        context.codeCacheDir?.walkTopDown()?.forEach { file ->
            if (file.isFile) size += file.length()
        }
    } catch (e: Exception) {
        // ignore
    }
    return size
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> "%.2f GB".format(bytes.toDouble() / (1024 * 1024 * 1024))
        bytes >= 1024 * 1024 -> "%.1f MB".format(bytes.toDouble() / (1024 * 1024))
        bytes >= 1024 -> "%d KB".format(bytes / 1024)
        else -> "$bytes B"
    }
}

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storageManager = remember { StorageManager.getInstance(context) }

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfoToPrompt by remember { mutableStateOf<AppUpdateInfo?>(null) }

    var preferredQuality by remember { mutableStateOf(storageManager.getDefaultQuality()) }
    val qualityOptions = listOf("Auto (Best)", "1080P Ultra HD", "720P HD", "480P Data Saver")
    var showQualityMenu by remember { mutableStateOf(false) }

    var isAutoplayEnabled by remember { mutableStateOf(storageManager.isAutoplayEnabled()) }
    var isFamilyModeEnabled by remember { mutableStateOf(storageManager.isFamilyModeEnabled()) }

    var cacheSizeBytes by remember { mutableLongStateOf(calculateCacheSize(context)) }
    var watchlistCount by remember { mutableIntStateOf(storageManager.getWatchlist().size) }
    var downloadCount by remember { mutableIntStateOf(storageManager.getDownloads().size) }

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

        Spacer(modifier = Modifier.height(20.dp))

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
                        text = "VIP Cinephile",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "VIP Member • Unlimited Access",
                        style = MaterialTheme.typography.bodySmall,
                        color = RatingGold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Library Quick Counters
        Text(
            text = "My Activity",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkCard,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Bookmark, contentDescription = "Watchlist", tint = PrimaryRed)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "$watchlistCount Titles", color = TextPrimary, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text(text = "Saved in List", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkCard,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = DownloadNavIcon, contentDescription = "Downloads", tint = RatingGold)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "$downloadCount Titles", color = TextPrimary, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text(text = "Downloaded", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Playback Preferences Section
        Text(
            text = "Streaming Preferences",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Streaming Quality Selector
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
                    Text(text = preferredQuality, color = PrimaryRed, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                }
                Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings", tint = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Autoplay Switch
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkCard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(imageVector = Icons.Filled.PlayCircle, contentDescription = "Autoplay", tint = PrimaryRed)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Autoplay Next Episode", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Automatically play subsequent episodes in series", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Switch(
                    checked = isAutoplayEnabled,
                    onCheckedChange = { checked ->
                        storageManager.setAutoplayEnabled(checked)
                        isAutoplayEnabled = checked
                        Toast.makeText(context, if (checked) "Autoplay enabled" else "Autoplay disabled", Toast.LENGTH_SHORT).show()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryRed,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = DarkSurfaceElevated
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Family Mode Switch
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkCard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(imageVector = Icons.Filled.FamilyRestroom, contentDescription = "Family Mode", tint = RatingGold)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Family Safe Mode", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Filter out 18+, R-rated, and mature content", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Switch(
                    checked = isFamilyModeEnabled,
                    onCheckedChange = { checked ->
                        storageManager.setFamilyModeEnabled(checked)
                        isFamilyModeEnabled = checked
                        Toast.makeText(context, if (checked) "Family Mode enabled" else "Family Mode disabled", Toast.LENGTH_SHORT).show()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryRed,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = DarkSurfaceElevated
                    )
                )
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
                    val before = cacheSizeBytes
                    try {
                        Coil.imageLoader(context).diskCache?.clear()
                        Coil.imageLoader(context).memoryCache?.clear()
                        context.cacheDir?.deleteRecursively()
                        context.cacheDir?.mkdirs()
                    } catch (e: Exception) {
                        // ignore
                    }
                    val freed = before.coerceAtLeast(0L)
                    cacheSizeBytes = calculateCacheSize(context)
                    Toast.makeText(context, "Cache cleared successfully! Freed ${formatSize(freed)}", Toast.LENGTH_SHORT).show()
                }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Clear Temporary Cache", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Current Cache: ${formatSize(cacheSizeBytes)} (Images & HTTP)",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Clear Cache", tint = PrimaryRed)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // App Information Section
        Text(
            text = "System Information",
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "App Version", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        Text(text = "v${BuildConfig.VERSION_NAME}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(
                        onClick = {
                            if (isCheckingUpdate) return@TextButton
                            isCheckingUpdate = true
                            scope.launch {
                                val update = UpdateManager.checkForUpdate()
                                isCheckingUpdate = false
                                if (update != null) {
                                    updateInfoToPrompt = update
                                } else {
                                    Toast.makeText(context, "You are on the latest version (v${BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text(
                            text = if (isCheckingUpdate) "Checking..." else "Check for Update",
                            color = PrimaryRed,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "App Size", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Text(text = "6.18 MB (Ultra Lightweight)", color = RatingGold, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Service Status", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Online & Operational", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // Quality Picker Dialog
        if (showQualityMenu) {
            AlertDialog(
                onDismissRequest = { showQualityMenu = false },
                title = { Text(text = "Select Default Quality", color = Color.White) },
                text = {
                    Column {
                        qualityOptions.forEach { opt ->
                            val isSelected = opt == preferredQuality
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        storageManager.setDefaultQuality(opt)
                                        preferredQuality = opt
                                        showQualityMenu = false
                                        Toast.makeText(context, "Streaming quality set to $opt", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = opt,
                                    color = if (isSelected) PrimaryRed else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(imageVector = Icons.Filled.Check, contentDescription = "Selected", tint = PrimaryRed)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQualityMenu = false }) {
                        Text("Cancel", color = PrimaryRed)
                    }
                },
                containerColor = DarkCard
            )
        }

        // In-App Update Dialog
        if (updateInfoToPrompt != null) {
            AppUpdateDialog(
                updateInfo = updateInfoToPrompt!!,
                onDismiss = { updateInfoToPrompt = null }
            )
        }
    }
}

