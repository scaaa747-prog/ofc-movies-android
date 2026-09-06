package com.ofc.movies.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.ofc.movies.MainActivity
import com.ofc.movies.R
import com.ofc.movies.data.api.MovieBoxSigner
import com.ofc.movies.data.local.StorageManager
import com.ofc.movies.data.model.formatDownloadSize
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.dash.offline.DashDownloader
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
class DownloadService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var downloadManager: AppDownloadManager
    private lateinit var storageManager: StorageManager
    private lateinit var notificationManager: NotificationManager

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    companion object {
        const val CHANNEL_ID = "ofc_downloads_channel"
        const val NOTIFICATION_ID = 9991
    }

    override fun onCreate() {
        super.onCreate()
        downloadManager = AppDownloadManager.getInstance(this)
        storageManager = StorageManager.getInstance(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            createNotificationChannel()
            val initialNotif = buildNotification("Preparing download...", 0, 0L, 0L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    initialNotif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, initialNotif)
            }
        } catch (e: Throwable) {
            android.util.Log.e("DownloadService", "Failed to start foreground service", e)
        }

        startDownloadingQueue()
        return START_NOT_STICKY
    }

    private val isProcessingQueue = java.util.concurrent.atomic.AtomicBoolean(false)

    private fun startDownloadingQueue() {
        serviceScope.launch {
            if (!isProcessingQueue.compareAndSet(false, true)) {
                return@launch
            }
            try {
                while (true) {
                    val task = downloadManager.taskQueue.poll() ?: break
                    if (downloadManager.isCancelled(task.id)) {
                        downloadManager.clearCancelled(task.id)
                        continue
                    }

                    downloadManager.currentRunningTaskId = task.id
                    storageManager.updateDownloadStatus(task.id, "Downloading")

                    val success = downloadSingleTask(task)
                    if (success) {
                        try {
                            val completedNotif = NotificationCompat.Builder(this@DownloadService, CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_download_notif)
                                .setContentTitle("OFC Movies")
                                .setContentText("Downloaded: ${task.displayTitle}")
                                .setContentIntent(createOpenDownloadsPendingIntent())
                                .setAutoCancel(true)
                                .build()
                            notificationManager.notify(task.id.hashCode(), completedNotif)
                        } catch (e: Throwable) {
                            android.util.Log.e("DownloadService", "Failed to show completed notification", e)
                        }
                    } else {
                        try {
                            val failedNotif = NotificationCompat.Builder(this@DownloadService, CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_download_notif)
                                .setContentTitle("OFC Movies: Download Failed")
                                .setContentText("Failed to download ${task.displayTitle}")
                                .setContentIntent(createOpenDownloadsPendingIntent())
                                .setAutoCancel(true)
                                .build()
                            notificationManager.notify(task.id.hashCode(), failedNotif)
                        } catch (e: Throwable) {
                            android.util.Log.e("DownloadService", "Failed to show failed notification", e)
                        }
                    }

                    downloadManager.removeProgress(task.id)
                    downloadManager.currentRunningTaskId = null
                }
            } finally {
                isProcessingQueue.set(false)
                if (downloadManager.taskQueue.isEmpty()) {
                    try {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } catch (e: Throwable) {}
                    stopSelf()
                } else {
                    startDownloadingQueue()
                }
            }
        }
    }

    private fun downloadSingleTask(task: DownloadTask): Boolean {
        if (task.streamUrl.contains(".mpd")) {
            return downloadDashTask(task)
        }
        return downloadProgressiveTask(task)
    }

    private fun downloadDashTask(task: DownloadTask): Boolean {
        val executor = Executors.newFixedThreadPool(2)
        var downloader: DashDownloader? = null
        return try {
            val mediaItem = MediaItem.Builder()
                .setUri(task.streamUrl)
                .setMimeType(MimeTypes.APPLICATION_MPD)
                .build()

            val cacheFactory = DownloadCacheManager.createCacheDataSourceFactory(this, task.signCookie)
            downloader = DashDownloader(mediaItem, cacheFactory, executor)

            val estimatedTotal = when {
                task.estimatedSizeBytes > 0 -> task.estimatedSizeBytes
                task.sizeText.isNotEmpty() -> parseSizeTextToBytes(task.sizeText)
                else -> 0L
            }

            downloadManager.updateProgress(
                task.id,
                DownloadProgress(
                    taskId = task.id,
                    bytesDownloaded = 0L,
                    totalBytes = estimatedTotal,
                    percentage = 0,
                    status = "Downloading"
                )
            )

            var lastUpdateMs = 0L
            var maxBytesDownloaded = 0L

            downloader.download { contentLength, bytesDownloaded, percentDownloaded ->
                if (downloadManager.isCancelled(task.id)) {
                    try { downloader?.cancel() } catch (e: Exception) {}
                    return@download
                }

                maxBytesDownloaded = maxOf(maxBytesDownloaded, bytesDownloaded)
                val now = System.currentTimeMillis()
                if (now - lastUpdateMs > 250) {
                    val total = if (contentLength > 0) contentLength else estimatedTotal
                    val percent = when {
                        percentDownloaded in 0.01f..100.0f -> percentDownloaded.toInt().coerceIn(0, 100)
                        total > 0 && bytesDownloaded > 0 -> ((bytesDownloaded * 100) / total).toInt().coerceIn(0, 99)
                        else -> 0
                    }
                    downloadManager.updateProgress(
                        task.id,
                        DownloadProgress(
                            taskId = task.id,
                            bytesDownloaded = bytesDownloaded,
                            totalBytes = total,
                            percentage = percent,
                            status = "Downloading"
                        )
                    )

                    val notif = buildNotification(task.displayTitle, percent, bytesDownloaded, total)
                    try {
                        notificationManager.notify(NOTIFICATION_ID, notif)
                    } catch (e: Throwable) {}
                    lastUpdateMs = now
                }
            }

            if (downloadManager.isCancelled(task.id)) {
                return false
            }

            val finalTotal = if (maxBytesDownloaded > 0) maxBytesDownloaded else estimatedTotal
            downloadManager.updateProgress(
                task.id,
                DownloadProgress(
                    taskId = task.id,
                    bytesDownloaded = finalTotal,
                    totalBytes = finalTotal,
                    percentage = 100,
                    status = "Ready"
                )
            )

            val finalSizeText = if (maxBytesDownloaded > 0) formatDownloadSize(maxBytesDownloaded, 0) else task.sizeText
            storageManager.updateDownloadStatus(
                id = task.id,
                status = "Ready",
                localUri = "cache://${task.streamUrl}",
                sizeText = finalSizeText
            )
            true
        } catch (e: Exception) {
            android.util.Log.e("DownloadService", "DASH download failed for ${task.displayTitle}", e)
            storageManager.updateDownloadStatus(task.id, "Failed")
            false
        } finally {
            executor.shutdown()
        }
    }

    private fun downloadProgressiveTask(task: DownloadTask): Boolean {
        var outputStream: OutputStream? = null
        var createdUri: Uri? = null

        return try {
            val cleanCookie = task.signCookie?.replace("\r", "")?.replace("\n", "")?.trim()?.trimEnd(';') ?: ""
            val req = Request.Builder()
                .url(task.streamUrl)
                .header("User-Agent", MovieBoxSigner.ANDROID_USER_AGENT)
                .header("Referer", "https://www.movieboxpro.app/")
                .apply {
                    if (cleanCookie.isNotEmpty()) {
                        header("Cookie", cleanCookie)
                    }
                }
                .build()

            val response = okHttpClient.newCall(req).execute()
            android.util.Log.d("DownloadService", "Download response code: ${response.code} for ${task.displayTitle}")
            if (!response.isSuccessful) {
                android.util.Log.e("DownloadService", "Download failed: HTTP ${response.code} ${response.message}")
                storageManager.updateDownloadStatus(task.id, "Failed")
                return false
            }

            val body = response.body ?: return false
            val contentLength = body.contentLength().coerceAtLeast(0L)

            val safeTitle = task.displayTitle.replace(Regex("[^a-zA-Z0-9._ -]"), "_")
            val fileName = "${safeTitle}_${task.quality}.mp4"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ofcmovies/")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    createdUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (createdUri != null) {
                        outputStream = contentResolver.openOutputStream(createdUri)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DownloadService", "MediaStore insert failed, falling back to private storage", e)
                }
            }

            // Fallback for Android < Q or if MediaStore insert was restricted
            if (outputStream == null) {
                val baseDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val ofcDir = File(baseDir, "ofcmovies")
                if (!ofcDir.exists()) ofcDir.mkdirs()
                val file = File(ofcDir, fileName)
                outputStream = FileOutputStream(file)
                createdUri = Uri.fromFile(file)
            }

            val out = outputStream ?: return false

            // Immediately set status as Downloading with total bytes
            downloadManager.updateProgress(
                task.id,
                DownloadProgress(
                    taskId = task.id,
                    bytesDownloaded = 0L,
                    totalBytes = contentLength,
                    percentage = 0,
                    status = "Downloading"
                )
            )

            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            var totalBytesDownloaded = 0L
            var lastUpdateMs = 0L
            val inputStream = body.byteStream()

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (downloadManager.isCancelled(task.id)) {
                    out.close()
                    outputStream = null
                    if (createdUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentResolver.delete(createdUri, null, null)
                    }
                    return false
                }

                out.write(buffer, 0, bytesRead)
                totalBytesDownloaded += bytesRead

                val now = System.currentTimeMillis()
                if (now - lastUpdateMs > 300) {
                    val percent = if (contentLength > 0) ((totalBytesDownloaded * 100) / contentLength).toInt().coerceIn(0, 100) else 0
                    val progress = DownloadProgress(
                        taskId = task.id,
                        bytesDownloaded = totalBytesDownloaded,
                        totalBytes = contentLength,
                        percentage = percent,
                        status = "Downloading"
                    )
                    downloadManager.updateProgress(task.id, progress)

                    val notif = buildNotification(task.displayTitle, percent, totalBytesDownloaded, contentLength)
                    try {
                        notificationManager.notify(NOTIFICATION_ID, notif)
                    } catch (e: Throwable) {
                        android.util.Log.e("DownloadService", "Failed to update notification progress", e)
                    }
                    lastUpdateMs = now
                }
            }

            out.flush()
            out.close()
            outputStream = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && createdUri != null) {
                try {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    contentResolver.update(createdUri, values, null, null)
                } catch (e: Exception) {}
            }

            val sizeFormatted = formatDownloadSize(totalBytesDownloaded, 0)
            storageManager.updateDownloadStatus(
                id = task.id,
                status = "Ready",
                localUri = createdUri?.toString() ?: "",
                sizeText = sizeFormatted
            )
            true
        } catch (e: Exception) {
            android.util.Log.e("DownloadService", "Error during download task", e)
            storageManager.updateDownloadStatus(task.id, "Failed")
            false
        } finally {
            try { outputStream?.close() } catch (e: Exception) {}
        }
    }

    private fun buildNotification(
        title: String,
        percent: Int,
        downloadedBytes: Long,
        totalBytes: Long
    ): android.app.Notification {
        val contentText = if (totalBytes > 0) {
            val dStr = formatDownloadSize(downloadedBytes, 0)
            val tStr = formatDownloadSize(totalBytes, 0)
            "$percent% • $dStr / $tStr"
        } else {
            "Downloading..."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download_notif)
            .setContentTitle("OFC Movies: $title")
            .setContentText(contentText)
            .setProgress(100, percent, totalBytes <= 0)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(createOpenDownloadsPendingIntent())
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createOpenDownloadsPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_tab", "downloads")
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OFC Movies Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows movie and episode download progress"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun parseSizeTextToBytes(sizeText: String): Long {
        return try {
            val trimmed = sizeText.trim()
            val parts = trimmed.split(" ")
            if (parts.size >= 2) {
                val num = parts[0].toDoubleOrNull() ?: return 0L
                val unit = parts[1].uppercase()
                when {
                    unit.contains("GB") -> (num * 1024 * 1024 * 1024).toLong()
                    unit.contains("MB") -> (num * 1024 * 1024).toLong()
                    unit.contains("KB") -> (num * 1024).toLong()
                    else -> num.toLong()
                }
            } else 0L
        } catch (e: Exception) {
            0L
        }
    }
}
