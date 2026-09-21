package com.bettertube.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.bettertube.app.data.repository.Aria2Repository
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
