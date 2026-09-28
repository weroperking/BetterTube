package com.bettertube.app.data.aria2.rpc

import android.util.Log
import com.bettertube.app.data.aria2.Aria2ProcessManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Aria2RpcClient @Inject constructor(
    private val processManager: Aria2ProcessManager
) {
    companion object {
        private const val TAG = "Aria2RpcClient"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private fun getRpcUrl(): String = "http://127.0.0.1:${processManager.getRpcPort()}/jsonrpc"
    private fun getSecretToken(): String = "token:${processManager.getRpcSecret()}"

    private suspend fun <T> call(method: String, params: List<Any>, clazz: Class<T>): T? = withContext(Dispatchers.IO) {
        try {
            val fullParams = listOf(getSecretToken()) + params
            val rpcRequest = JsonRpcRequest(
                method = method,
                params = fullParams
            )
            val requestAdapter = moshi.adapter(JsonRpcRequest::class.java)
            val jsonString = requestAdapter.toJson(rpcRequest)

            val request = Request.Builder()
                .url(getRpcUrl())
                .post(jsonString.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "RPC request failed: HTTP ${response.code}")
                return@withContext null
            }

            val bodyString = response.body?.string() ?: return@withContext null
            val responseType = Types.newParameterizedType(JsonRpcResponse::class.java, clazz)
            val responseAdapter = moshi.adapter<JsonRpcResponse<T>>(responseType)
            val rpcResponse = responseAdapter.fromJson(bodyString)

            if (rpcResponse?.error != null) {
                Log.w(TAG, "RPC Error: [${rpcResponse.error.code}] ${rpcResponse.error.message}")
                return@withContext null
            }
            rpcResponse?.result
        } catch (e: Exception) {
            Log.w(TAG, "RPC call to $method failed", e)
            null
        }
    }

    private suspend fun <T> callList(method: String, params: List<Any>, clazz: Class<T>): List<T>? = withContext(Dispatchers.IO) {
        try {
            val fullParams = listOf(getSecretToken()) + params
            val rpcRequest = JsonRpcRequest(
                method = method,
                params = fullParams
            )
            val requestAdapter = moshi.adapter(JsonRpcRequest::class.java)
            val jsonString = requestAdapter.toJson(rpcRequest)

            val request = Request.Builder()
                .url(getRpcUrl())
                .post(jsonString.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "RPC request failed: HTTP ${response.code}")
                return@withContext null
            }

            val bodyString = response.body?.string() ?: return@withContext null
            val listType = Types.newParameterizedType(List::class.java, clazz)
            val responseType = Types.newParameterizedType(JsonRpcResponse::class.java, listType)
            val responseAdapter = moshi.adapter<JsonRpcResponse<List<T>>>(responseType)
            val rpcResponse = responseAdapter.fromJson(bodyString)

            if (rpcResponse?.error != null) {
                Log.w(TAG, "RPC Error: [${rpcResponse.error.code}] ${rpcResponse.error.message}")
                return@withContext null
            }
            rpcResponse?.result
        } catch (e: Exception) {
            Log.w(TAG, "RPC callList to $method failed", e)
            null
        }
    }

    suspend fun getVersion(): Aria2Version? {
        return call("aria2.getVersion", emptyList(), Aria2Version::class.java)
    }

    suspend fun getSessionInfo(): Aria2SessionInfo? {
        return call("aria2.getSessionInfo", emptyList(), Aria2SessionInfo::class.java)
    }

    suspend fun getGlobalStat(): Aria2GlobalStat? {
        return call("aria2.getGlobalStat", emptyList(), Aria2GlobalStat::class.java)
    }

    suspend fun addUri(uri: String, options: Map<String, Any> = emptyMap()): String? {
        return call("aria2.addUri", listOf(listOf(uri), options), String::class.java)
    }

    suspend fun addTorrent(
        torrentBase64: String,
        uris: List<String> = emptyList(),
        options: Map<String, Any> = emptyMap()
    ): String? {
        return call("aria2.addTorrent", listOf(torrentBase64, uris, options), String::class.java)
    }

    suspend fun addMetalink(
        metalinkBase64: String,
        options: Map<String, Any> = emptyMap()
    ): List<String>? {
        return callList("aria2.addMetalink", listOf(metalinkBase64, options), String::class.java)
    }

    suspend fun remove(gid: String): String? {
        return call("aria2.remove", listOf(gid), String::class.java)
    }

    suspend fun forceRemove(gid: String): String? {
        return call("aria2.forceRemove", listOf(gid), String::class.java)
    }

    suspend fun pause(gid: String): String? {
        return call("aria2.pause", listOf(gid), String::class.java)
    }

    suspend fun pauseAll(): String? {
        return call("aria2.pauseAll", emptyList(), String::class.java)
    }

    suspend fun unpause(gid: String): String? {
        return call("aria2.unpause", listOf(gid), String::class.java)
    }

    suspend fun unpauseAll(): String? {
        return call("aria2.unpauseAll", emptyList(), String::class.java)
    }

    suspend fun tellStatus(gid: String): Aria2Status? {
        return call("aria2.tellStatus", listOf(gid), Aria2Status::class.java)
    }

    suspend fun tellActive(): List<Aria2Status>? {
        return callList("aria2.tellActive", emptyList(), Aria2Status::class.java)
    }

    suspend fun tellWaiting(offset: Int = 0, num: Int = 100): List<Aria2Status>? {
        return callList("aria2.tellWaiting", listOf(offset, num), Aria2Status::class.java)
    }

    suspend fun tellStopped(offset: Int = 0, num: Int = 100): List<Aria2Status>? {
        return callList("aria2.tellStopped", listOf(offset, num), Aria2Status::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getOption(gid: String): Map<String, String>? {
        return call("aria2.getOption", listOf(gid), Map::class.java) as? Map<String, String>
    }

    suspend fun changeOption(gid: String, options: Map<String, Any>): String? {
        return call("aria2.changeOption", listOf(gid, options), String::class.java)
    }

    suspend fun changeGlobalOption(options: Map<String, Any>): String? {
        return call("aria2.changeGlobalOption", listOf(options), String::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getGlobalOption(): Map<String, String>? {
        return call("aria2.getGlobalOption", emptyList(), Map::class.java) as? Map<String, String>
    }

    suspend fun purgeDownloadResult(): String? {
        return call("aria2.purgeDownloadResult", emptyList(), String::class.java)
    }

    suspend fun removeDownloadResult(gid: String): String? {
        return call("aria2.removeDownloadResult", listOf(gid), String::class.java)
    }

    suspend fun saveSession(): String? {
        return call("aria2.saveSession", emptyList(), String::class.java)
    }
}
