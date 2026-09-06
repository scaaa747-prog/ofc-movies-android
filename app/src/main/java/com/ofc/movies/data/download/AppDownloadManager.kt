package com.ofc.movies.data.download

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.ofc.movies.data.local.DownloadedItem
import com.ofc.movies.data.local.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

data class DownloadTask(
    val id: String,
    val movieId: String,
    val title: String,
    val displayTitle: String,
    val coverUrl: String,
    val streamUrl: String,
    val quality: String,
    val sizeText: String,
    val signCookie: String? = null,
    val season: Int = 0,
    val episode: Int = 0
)

data class DownloadProgress(
    val taskId: String,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val percentage: Int = 0,
    val status: String = "Queued"
)

class AppDownloadManager private constructor(private val appContext: Context) {

    private val storageManager = StorageManager.getInstance(appContext)
    val taskQueue = ConcurrentLinkedQueue<DownloadTask>()
    private val cancelledTaskIds = ConcurrentHashMap.newKeySet<String>()

    private val _progressMap = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val progressMap: StateFlow<Map<String, DownloadProgress>> = _progressMap.asStateFlow()

    @Volatile
    var currentRunningTaskId: String? = null

    companion object {
        @Volatile
        private var instance: AppDownloadManager? = null

        fun getInstance(context: Context): AppDownloadManager {
            return instance ?: synchronized(this) {
                instance ?: AppDownloadManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun enqueueTasks(tasks: List<DownloadTask>) {
        if (tasks.isEmpty()) return

        for (task in tasks) {
            cancelledTaskIds.remove(task.id)
            // Persist item as Queued in StorageManager
            storageManager.addDownload(
                DownloadedItem(
                    id = task.id,
                    title = task.displayTitle,
                    coverUrl = task.coverUrl,
                    sizeText = task.sizeText,
                    quality = task.quality,
                    downloadTimeMs = System.currentTimeMillis(),
                    streamUrl = task.streamUrl,
                    downloadId = -1L,
                    localUri = "",
                    status = "Queued"
                )
            )

            updateProgress(
                task.id,
                DownloadProgress(
                    taskId = task.id,
                    bytesDownloaded = 0L,
                    totalBytes = 0L,
                    percentage = 0,
                    status = "Queued"
                )
            )

            taskQueue.add(task)
        }

        // Trigger Foreground DownloadService safely
        try {
            val intent = Intent(appContext, DownloadService::class.java)
            ContextCompat.startForegroundService(appContext, intent)
        } catch (e: Throwable) {
            android.util.Log.e("AppDownloadManager", "startForegroundService failed, falling back to startService", e)
            try {
                val intent = Intent(appContext, DownloadService::class.java)
                appContext.startService(intent)
            } catch (e2: Throwable) {
                android.util.Log.e("AppDownloadManager", "startService fallback also failed", e2)
            }
        }
    }

    fun updateProgress(taskId: String, progress: DownloadProgress) {
        val current = _progressMap.value.toMutableMap()
        current[taskId] = progress
        _progressMap.value = current
    }

    fun removeProgress(taskId: String) {
        val current = _progressMap.value.toMutableMap()
        current.remove(taskId)
        _progressMap.value = current
    }

    fun cancelTask(taskId: String) {
        cancelledTaskIds.add(taskId)
        taskQueue.removeIf { it.id == taskId }
        removeProgress(taskId)
        storageManager.removeDownload(taskId)
    }

    fun isCancelled(taskId: String): Boolean = cancelledTaskIds.contains(taskId)

    fun clearCancelled(taskId: String) {
        cancelledTaskIds.remove(taskId)
    }
}
