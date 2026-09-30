package com.bettertube.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.bettertube.app.domain.repository.Aria2Repository
import com.bettertube.app.utils.CrashLogger
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

// Verified against youtubedl-android 0.15.0 AAR on 2026-09-30:
// - YoutubeDL.init(Context): EXISTS
// - YoutubeDL.updateYoutubeDL(Context, YoutubeDL.UpdateChannel): EXISTS but forbidden by remediation constraints
// - Process.pid(): API 26+ only — not used (minSdk is 24)

@HiltAndroidApp
class BetterTubeApp : Application(), Configuration.Provider {

    @Inject
    lateinit var hiltWorkerFactory: HiltWorkerFactory

    @Inject
    lateinit var aria2Repository: Aria2Repository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .build()

    @Volatile
    var engineReady: Boolean = false

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        instance = this
        applicationScope.launch {
            try {
                YoutubeDL.getInstance().init(applicationContext)
                FFmpeg.getInstance().init(applicationContext)
                engineReady = true
                Log.i(TAG, "YoutubeDL and FFmpeg engines initialized successfully.")
                try {
                    val version = YoutubeDL.getInstance().version(applicationContext)
                    Log.i(TAG, "yt-dlp version: $version")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not read yt-dlp version: ${e.message}")
                }
                // Note: runtime yt-dlp update is not used in this remediation.
                // YouTube compatibility is handled via extractor args in YtDlpEngine.
            } catch (e: Exception) {
                engineReady = false
                Log.e(TAG, "Failed to initialize YoutubeDL / FFmpeg engine", e)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
        try {
            aria2Repository.close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to close aria2 repository", e)
        }
    }

    fun isEngineReady(): Boolean = engineReady

    companion object {
        private const val TAG = "BetterTubeApp"
        private var instance: BetterTubeApp? = null

        fun isEngineReady(): Boolean = instance?.isEngineReady() ?: false
    }
}
