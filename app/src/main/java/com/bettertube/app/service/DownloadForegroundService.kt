package com.bettertube.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.bettertube.app.MainActivity
import com.bettertube.app.R
import com.bettertube.app.domain.model.DownloadStatus
import com.bettertube.app.domain.model.DownloadTask
import com.bettertube.app.domain.repository.DownloadRepository
import com.bettertube.app.utils.FormatUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class DownloadForegroundService : Service() {

    @Inject
    lateinit var downloadRepository: DownloadRepository

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var lastNotificationTimeMs = 0L
    private var lastPostedPercent = -1
    private var lastPostedSpeed = -1L
    private var lastPostedActiveCount = -1

    companion object {
        const val CHANNEL_ID = "bettertube_downloads"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.bettertube.app.START"
        const val ACTION_STOP = "com.bettertube.app.STOP"

        fun start(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        serviceScope.launch {
            downloadRepository.getAllTasks().collect { tasks ->
                val activeTasks = tasks.filter {
                    it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.WAITING
                }

                if (activeTasks.isEmpty()) {
                    removeForegroundAndStop()
                } else {
                    if (shouldPostUpdate(activeTasks)) {
                        val notification = buildNotification(activeTasks)
                        if (canPostNotification()) {
                            NotificationManagerCompat.from(this@DownloadForegroundService)
                                .notify(NOTIFICATION_ID, notification)
                        }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val initialNotification = buildNotification(emptyList())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        initialNotification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(NOTIFICATION_ID, initialNotification)
                }
            }
            ACTION_STOP -> {
                removeForegroundAndStop()
            }
        }
        return START_STICKY
    }

    private fun removeForegroundAndStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Download progress notifications"
                enableVibration(false)
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun shouldPostUpdate(activeTasks: List<DownloadTask>): Boolean {
        val count = activeTasks.size
        if (count != lastPostedActiveCount) {
            recordMetrics(activeTasks)
            return true
        }
        if (count == 0) return true

        val now = SystemClock.elapsedRealtime()
        if (now - lastNotificationTimeMs < 1000L) return false

        if (count == 1) {
            val task = activeTasks.first()
            val percent = (task.progressPercent * 100).toInt()
            val speed = task.speedBytesPerSecond
            val percentDiff = abs(percent - lastPostedPercent)
            val speedDiff = abs(speed - lastPostedSpeed)
            if (percentDiff >= 1 || speedDiff >= 10 * 1024L) {
                recordMetrics(activeTasks)
                return true
            }
        } else {
            val totalSpeed = activeTasks.sumOf { it.speedBytesPerSecond }
            val speedDiff = abs(totalSpeed - lastPostedSpeed)
            if (speedDiff >= 10 * 1024L) {
                recordMetrics(activeTasks)
                return true
            }
        }
        return false
    }

    private fun recordMetrics(activeTasks: List<DownloadTask>) {
        lastNotificationTimeMs = SystemClock.elapsedRealtime()
        lastPostedActiveCount = activeTasks.size
        if (activeTasks.size == 1) {
            val task = activeTasks.first()
            lastPostedPercent = (task.progressPercent * 100).toInt()
            lastPostedSpeed = task.speedBytesPerSecond
        } else {
            lastPostedPercent = -1
            lastPostedSpeed = activeTasks.sumOf { it.speedBytesPerSecond }
        }
    }

    private fun canPostNotification(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun buildNotification(tasks: List<DownloadTask>): Notification {
        val activeCount = tasks.count { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.WAITING }
        val downloadingTask = tasks.firstOrNull { it.status == DownloadStatus.DOWNLOADING } ?: tasks.firstOrNull()

        val title = when {
            activeCount <= 1 -> "BetterTube — Downloading"
            else -> "BetterTube — $activeCount downloads"
        }

        val text = when {
            activeCount == 0 -> "Downloads in progress"
            activeCount == 1 && downloadingTask != null -> {
                val percent = (downloadingTask.progressPercent * 100).toInt()
                val speed = FormatUtils.formatSpeed(downloadingTask.speedBytesPerSecond)
                "${downloadingTask.title} — $percent% — $speed"
            }
            else -> {
                val totalSpeed = tasks.sumOf { it.speedBytesPerSecond }
                "$activeCount active · ${FormatUtils.formatSpeed(totalSpeed)}"
            }
        }

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("navigate_to", "downloads")
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingContentIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, DownloadForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setContentIntent(pendingContentIntent)
            .addAction(0, "Stop service", pendingStopIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (activeCount == 1 && downloadingTask != null) {
            val percent = (downloadingTask.progressPercent * 100).toInt()
            builder.setProgress(100, percent, false)
        }

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
