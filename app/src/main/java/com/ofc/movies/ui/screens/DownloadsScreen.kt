package com.ofc.movies.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
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
import coil.compose.AsyncImage
import com.ofc.movies.data.api.MovieBoxSigner
import com.ofc.movies.data.download.AppDownloadManager
import com.ofc.movies.data.local.DownloadedItem
import com.ofc.movies.data.local.StorageManager
import com.ofc.movies.data.model.formatDownloadSize
import com.ofc.movies.ui.components.DownloadNavIcon
import com.ofc.movies.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun DownloadsScreen(
    onPlayOffline: (movieId: String, title: String) -> Unit,
    onBrowseMovies: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storageManager = remember { StorageManager.getInstance(context) }
    val appDownloadManager = remember { AppDownloadManager.getInstance(context) }
    val inAppProgressMap by appDownloadManager.progressMap.collectAsState()

    var downloads by remember { mutableStateOf(storageManager.getDownloads()) }
    var downloadStatuses by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }

    // Auto-refresh downloads list whenever inAppProgressMap updates
    LaunchedEffect(inAppProgressMap) {
        downloads = storageManager.getDownloads()
    }


    // Periodically refresh real download statuses
    LaunchedEffect(Unit) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        while (true) {
            downloads = storageManager.getDownloads()
            if (dm != null && downloads.isNotEmpty()) {
                val map = mutableMapOf<Long, String>()
                downloads.forEach { item ->
                    if (item.downloadId > 0L) {
                        try {
                            val cursor = dm.query(DownloadManager.Query().setFilterById(item.downloadId))
                            if (cursor != null && cursor.moveToFirst()) {
                                val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                                val status = if (statusIdx != -1) cursor.getInt(statusIdx) else -1
                                val bytesDownloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                                val bytesTotalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                                val downloaded = if (bytesDownloadedIdx != -1) cursor.getLong(bytesDownloadedIdx) else 0L
                                val total = if (bytesTotalIdx != -1) cursor.getLong(bytesTotalIdx) else 0L

                                val statusStr = when (status) {
                                    DownloadManager.STATUS_RUNNING -> {
                                        if (total > 0) "${(downloaded * 100 / total)}%" else "Downloading"
                                    }
                                    DownloadManager.STATUS_PENDING -> "Queued"
                                    DownloadManager.STATUS_SUCCESSFUL -> "Ready"
                                    DownloadManager.STATUS_FAILED -> "Failed"
                                    else -> "Ready"
                                }
                                map[item.downloadId] = statusStr
                                cursor.close()
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                }
                downloadStatuses = map
            }
            delay(1500)
        }
    }

    // Calculate real device storage via StatFs
    val (freeSpaceText, totalSpaceText, usedRatio) = remember {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val available = stat.availableBlocksLong * stat.blockSizeLong
            val total = stat.blockCountLong * stat.blockSizeLong
            val used = total - available
            val freeGb = available.toDouble() / (1024 * 1024 * 1024)
            val totalGb = total.toDouble() / (1024 * 1024 * 1024)
            val ratio = if (total > 0) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0.5f
            Triple("%.1f GB Free".format(freeGb), "%.1f GB Total".format(totalGb), ratio)
        } catch (e: Exception) {
            Triple("Available", "Device Storage", 0.3f)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
    ) {
        Text(
            text = "Downloads",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            ),
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        // Real Device Storage Bar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkCard,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Device Storage", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                    Text(text = freeSpaceText, color = RatingGold, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { usedRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PrimaryRed,
                    trackColor = DarkSurfaceElevated
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Real System Storage", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Text(text = totalSpaceText, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(DarkCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = DownloadNavIcon,
                            contentDescription = "No Downloads",
                            tint = TextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "No Downloads Yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Download movies and episodes to watch them offline anytime without internet connection.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onBrowseMovies,
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Text(
                            text = "Browse Movies",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(downloads, key = { it.id }) { item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkCard,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val inApp = inAppProgressMap[item.id]
                            val isDownloading = inApp?.status == "Downloading" || item.status == "Downloading"
                            val isQueued = inApp?.status == "Queued" || item.status == "Queued"
                            val isFailed = item.status == "Failed"
                            val isReady = item.status == "Ready" || (!isDownloading && !isQueued && !isFailed)

                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (isDownloading || isQueued) DarkSurfaceElevated else PrimaryRed)
                                    .clickable {
                                        if (isDownloading || isQueued) {
                                            Toast.makeText(context, "Download in progress. Please wait until finished.", Toast.LENGTH_SHORT).show()
                                        } else if (isFailed) {
                                            Toast.makeText(context, "Download failed. Please delete and retry.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            onPlayOffline(item.id, item.title)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDownloading) {
                                    val progressFraction = if (inApp != null && inApp.totalBytes > 0) {
                                        (inApp.bytesDownloaded.toFloat() / inApp.totalBytes.toFloat()).coerceIn(0f, 1f)
                                    } else 0f
                                    CircularProgressIndicator(
                                        progress = { progressFraction },
                                        modifier = Modifier.size(28.dp),
                                        color = PrimaryRed,
                                        strokeWidth = 3.dp,
                                        trackColor = DarkCard
                                    )
                                } else if (isQueued) {
                                    Icon(
                                        imageVector = DownloadNavIcon,
                                        contentDescription = "Queued",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.quality,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RatingGold
                                    )
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )

                                    if (isDownloading) {
                                        val downloadedStr = if (inApp != null) formatDownloadSize(inApp.bytesDownloaded, 0) else "0 MB"
                                        val totalStr = if (inApp != null && inApp.totalBytes > 0) {
                                            formatDownloadSize(inApp.totalBytes, 0)
                                        } else {
                                            item.sizeText
                                        }
                                        val pctStr = if (inApp != null && inApp.percentage > 0) "${inApp.percentage}%" else "Starting"
                                        Text(
                                            text = "$downloadedStr / $totalStr ($pctStr)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = PrimaryRed
                                        )
                                    } else if (isQueued) {
                                        Text(
                                            text = "${item.sizeText} • Queued in line",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    } else if (isFailed) {
                                        Text(
                                            text = "Download Failed",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = PrimaryRed
                                        )
                                    } else {
                                        Text(
                                            text = item.sizeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "Ready to Watch",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                }

                                if (isDownloading || isQueued) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val progressFraction = if (isDownloading && inApp != null && inApp.totalBytes > 0) {
                                        (inApp.bytesDownloaded.toFloat() / inApp.totalBytes.toFloat()).coerceIn(0f, 1f)
                                    } else if (isDownloading) {
                                        0.05f
                                    } else {
                                        0f
                                    }
                                    LinearProgressIndicator(
                                        progress = { progressFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = PrimaryRed,
                                        trackColor = DarkSurfaceElevated
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    appDownloadManager.cancelTask(item.id)
                                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                                    if (item.downloadId > 0L) {
                                        try {
                                            dm?.remove(item.downloadId)
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    }
                                    if (item.localUri.isNotEmpty()) {
                                        try {
                                            val uri = Uri.parse(item.localUri)
                                            if (uri.scheme == "content") {
                                                context.contentResolver.delete(uri, null, null)
                                            } else if (uri.scheme == "file") {
                                                File(uri.path ?: "").delete()
                                            }
                                        } catch (e: Exception) {}
                                    }
                                    if (item.streamUrl.isNotEmpty()) {
                                        try {
                                            com.ofc.movies.data.download.DownloadCacheManager.getCache(context).removeResource(item.streamUrl)
                                        } catch (e: Exception) {}
                                    }
                                    storageManager.removeDownload(item.id)
                                    downloads = storageManager.getDownloads()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
