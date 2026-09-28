package com.bettertube.app.data.aria2

import android.content.Context
import android.util.Log
import com.bettertube.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Aria2ProcessManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val binaryProvider: Aria2BinaryProvider
) {
    companion object {
        private const val TAG = "Aria2ProcessManager"
        const val RPC_PORT = 6800
        val RPC_SECRET = "bettertube_" + BuildConfig.APPLICATION_ID.hashCode().toString(16)
    }

    private val sessionFile = File(context.filesDir, "aria2.session")
    private val configFile = File(context.filesDir, "aria2.conf")
    private val logFile = File(context.filesDir, "aria2.log")
    private val settingsFile = File(context.filesDir, "settings.json")

    private var process: Process? = null
    private var currentPort: Int = RPC_PORT

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun getRpcSecret(): String = RPC_SECRET
    fun getRpcPort(): Int = currentPort

    fun markAsRunning(port: Int) {
        currentPort = port
        _isRunning.value = true
    }

    suspend fun start(): Result<Unit> = withContext(Dispatchers.IO) {
        if (process?.isAlive == true) {
            return@withContext Result.success(Unit)
        }

        if (!binaryProvider.ensureExecutable()) {
            return@withContext Result.failure(IllegalStateException("Cannot make aria2c executable"))
        }

        val binaryFile = try {
            binaryProvider.getAria2cPath()
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        val downloadDir = File(context.getExternalFilesDir(null), "BetterTube")
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }

        if (!sessionFile.exists()) {
            try {
                sessionFile.createNewFile()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to create session file", e)
            }
        }

        var lastError = "Unknown error"

        for (port in 6800..6810) {
            val command = listOf(
                binaryFile.absolutePath,
                "--enable-rpc=true",
                "--rpc-listen-all=false",
                "--rpc-listen-port=$port",
                "--rpc-secret=$RPC_SECRET",
                "--rpc-allow-origin-all=false",
                "--continue=true",
                "--max-concurrent-downloads=5",
                "--split=16",
                "--max-connection-per-server=16",
                "--min-split-size=1M",
                "--file-allocation=none",
                "--dir=${downloadDir.absolutePath}",
                "--save-session=${sessionFile.absolutePath}",
                "--save-session-interval=30",
                "--input-file=${sessionFile.absolutePath}",
                "--auto-save-interval=30",
                "--log=${logFile.absolutePath}",
                "--log-level=warn",
                "--summary-interval=1",
                "--console-log-level=error",
                "--bt-enable-lpd=true",
                "--enable-dht=true",
                "--enable-dht6=false",
                "--bt-max-peers=128",
                "--follow-torrent=true",
                "--seed-time=0",
                "--max-overall-upload-limit=1K",
                "--listen-port=6881-6999",
                "--dht-listen-port=6881-6999",
                "--max-file-not-found=5",
                "--max-tries=5"
            )

            try {
                val pb = ProcessBuilder(command)
                pb.redirectErrorStream(true)
                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
                val startedProcess = pb.start()
                process = startedProcess

                delay(500L)

                if (startedProcess.isAlive) {
                    currentPort = port
                    _isRunning.value = true
                    persistChosenPort(port)
                    Log.i(TAG, "aria2c daemon started successfully on port $port")
                    return@withContext Result.success(Unit)
                } else {
                    val tail = readLogTail(100)
                    lastError = tail
                    Log.w(TAG, "aria2c failed to stay alive on port $port: $tail")
                    startedProcess.destroy()
                }
            } catch (e: Exception) {
                lastError = e.message ?: "ProcessBuilder error"
                Log.w(TAG, "Exception starting aria2c on port $port", e)
            }
        }

        _isRunning.value = false
        Result.failure(IllegalStateException("aria2c exited on startup: $lastError"))
    }

    suspend fun stop(): Result<Unit> = withContext(Dispatchers.IO) {
        val p = process
        if (p != null) {
            p.destroy()
            try {
                if (!p.waitFor(2, TimeUnit.SECONDS)) {
                    p.destroyForcibly()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exception while waiting for aria2c to stop", e)
                p.destroyForcibly()
            }
            process = null
        }
        _isRunning.value = false
        Result.success(Unit)
    }

    suspend fun restart(): Result<Unit> {
        stop()
        return start()
    }

    private fun readLogTail(maxLines: Int): String {
        return try {
            if (!logFile.exists()) return "Log file does not exist"
            logFile.readLines().takeLast(maxLines).joinToString("\n")
        } catch (e: Exception) {
            "Could not read log file: ${e.message}"
        }
    }

    private fun persistChosenPort(port: Int) {
        try {
            val json = if (settingsFile.exists()) {
                try { JSONObject(settingsFile.readText()) } catch (e: Exception) { Log.w(TAG, "Failed to read settings.json for port persistence", e); JSONObject() }
            } else {
                JSONObject()
            }
            json.put("aria2_rpc_port", port)
            val tmpFile = File(context.filesDir, "settings.json.tmp")
            tmpFile.writeText(json.toString(2))
            tmpFile.renameTo(settingsFile)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist chosen port to settings.json", e)
        }
    }
}
