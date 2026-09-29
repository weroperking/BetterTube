package com.bettertube.app.data.aria2

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.bettertube.app.BuildConfig
import com.bettertube.app.data.aria2.rpc.Aria2RpcClient
import com.bettertube.app.data.aria2.rpc.Aria2Version
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Aria2ProcessManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val binaryProvider: Aria2BinaryProvider,
    private val rpcClientLazy: Lazy<Aria2RpcClient>
) {
    private val rpcClient: Aria2RpcClient
        get() = rpcClientLazy.get()
    companion object {
        private const val TAG = "Aria2ProcessManager"
        const val RPC_PORT = 6800
    }

    private val sessionFile = File(context.filesDir, "aria2.session")
    private val configFile = File(context.filesDir, "aria2.conf")
    private val logFile = File(context.filesDir, "aria2.log")
    private val settingsFile = File(context.filesDir, "settings.json")

    private var process: Process? = null
    private var currentPort: Int = RPC_PORT

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun getRpcSecret(): String = getOrCreateRpcSecret()
    fun getRpcPort(): Int = currentPort

    fun markAsRunning(port: Int) {
        currentPort = port
        _isRunning.value = true
    }

    private fun getDownloadDirectory(): File {
        val dir = File(context.getExternalFilesDir(null), "BetterTube")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    init {
        loadPersistedPort()
    }

    private fun loadPersistedPort() {
        try {
            if (settingsFile.exists()) {
                val json = JSONObject(settingsFile.readText())
                if (json.has("aria2_rpc_port")) {
                    currentPort = json.getInt("aria2_rpc_port")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load persisted aria2 port", e)
        }
    }

    private fun getOrCreateRpcSecret(): String {
        val prefs = EncryptedSharedPreferences.create(
            context,
            "bettertube_aria2_secret",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        prefs.getString("rpc_secret", null)?.let { return it }
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        val secret = Base64.encodeToString(bytes, Base64.NO_WRAP)
        prefs.edit().putString("rpc_secret", secret).apply()
        return secret
    }

    suspend fun start(): Result<Unit> = withContext(Dispatchers.IO) {
        if (process?.isAlive == true) {
            return@withContext Result.success(Unit)
        }

        if (rpcClient.getVersionOnPort(currentPort) != null) {
            markAsRunning(currentPort)
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

        val downloadDir = getDownloadDirectory()

        if (!sessionFile.exists()) {
            try {
                sessionFile.createNewFile()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to create session file", e)
            }
        }

        Log.i("Aria2PM", "=== aria2 startup diagnostic ===")
        Log.i("Aria2PM", "nativeLibraryDir = ${context.applicationInfo.nativeLibraryDir}")
        Log.i("Aria2PM", "filesDir = ${context.filesDir.absolutePath}")
        Log.i("Aria2PM", "binary path resolved to = ${binaryFile.absolutePath}")
        Log.i("Aria2PM", "binary exists = ${binaryFile.exists()}")
        Log.i("Aria2PM", "binary canExecute = ${binaryFile.canExecute()}")
        Log.i("Aria2PM", "binary length = ${binaryFile.length()}")
        Log.i("Aria2PM", "RPC port to use = $currentPort")
        Log.i("Aria2PM", "RPC secret length = ${getOrCreateRpcSecret().length}")
        Log.i("Aria2PM", "Download dir = ${getDownloadDirectory().absolutePath}")

        var lastError = "Unknown error"

        for (port in 6800..6810) {
            val command = listOf(
                binaryFile.absolutePath,
                "--enable-rpc=true",
                "--rpc-listen-all=false",
                "--rpc-listen-port=$port",
                "--rpc-secret=${getOrCreateRpcSecret()}",
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
                pb.environment()["LD_LIBRARY_PATH"] = "${context.noBackupFilesDir}/youtubedl-android/packages/aria2c/usr/lib:${context.applicationInfo.nativeLibraryDir}"
                pb.redirectErrorStream(true)
                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
                val startedProcess = pb.start()
                process = startedProcess

                val pid = try { startedProcess.pid() } catch (e: Throwable) { -1 }
                Log.i("Aria2PM", "Process started, PID = $pid")
                delay(500)
                Log.i("Aria2PM", "After 500ms, process alive = ${startedProcess.isAlive}")
                Log.i("Aria2PM", "Exit value (if dead) = ${runCatching { startedProcess.exitValue() }.getOrNull()}")

                val stderrTail = readLogTail(30)
                Log.i("Aria2PM", "aria2c stderr tail: $stderrTail")

                if (startedProcess.isAlive) {
                    currentPort = port
                    _isRunning.value = true
                    persistChosenPort(port)
                    Log.i(TAG, "aria2c daemon started successfully on port $port")
                    return@withContext Result.success(Unit)
                } else {
                    lastError = readLogTail(100)
                    Log.w(TAG, "aria2c failed to stay alive on port $port: $lastError")
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
        } else if (_isRunning.value) {
            rpcClient.shutdown()
            var attempts = 0
            while (rpcClient.getVersion() != null && attempts < 10) {
                delay(200)
                attempts++
            }
            if (rpcClient.getVersion() != null) {
                return@withContext Result.failure(IllegalStateException("aria2 daemon did not stop within timeout"))
            }
        }
        _isRunning.value = false
        logFile.delete()
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
