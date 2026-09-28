package com.bettertube.app.data.aria2

import com.bettertube.app.data.aria2.rpc.JsonRpcRequest
import com.bettertube.app.data.aria2.rpc.JsonRpcResponse
import com.bettertube.app.domain.model.Aria2DownloadStatus
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class Aria2JsonRpcTest {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun jsonRpcRequest_serializesProperly() {
        val adapter = moshi.adapter(JsonRpcRequest::class.java)
        val request = JsonRpcRequest(
            id = "test-123",
            method = "aria2.tellActive",
            params = listOf("token:mysecret")
        )
        val json = adapter.toJson(request)
        assertNotNull(json)
        assert(json.contains("\"jsonrpc\":\"2.0\""))
        assert(json.contains("\"id\":\"test-123\""))
        assert(json.contains("\"method\":\"aria2.tellActive\""))
    }

    @Test
    fun jsonRpcResponse_parsesSuccessResult() {
        val json = """
            {
                "jsonrpc": "2.0",
                "id": "1",
                "result": "OK"
            }
        """.trimIndent()
        val type = Types.newParameterizedType(JsonRpcResponse::class.java, String::class.java)
        val adapter = moshi.adapter<JsonRpcResponse<String>>(type)
        val response = adapter.fromJson(json)
        assertNotNull(response)
        assertEquals("1", response?.id)
        assertEquals("OK", response?.result)
        assertNull(response?.error)
    }

    @Test
    fun jsonRpcResponse_parsesErrorResult() {
        val json = """
            {
                "jsonrpc": "2.0",
                "id": "2",
                "error": {
                    "code": 1,
                    "message": "GID 1234 not found"
                }
            }
        """.trimIndent()
        val type = Types.newParameterizedType(JsonRpcResponse::class.java, String::class.java)
        val adapter = moshi.adapter<JsonRpcResponse<String>>(type)
        val response = adapter.fromJson(json)
        assertNotNull(response)
        assertEquals("2", response?.id)
        assertNull(response?.result)
        assertEquals(1, response?.error?.code)
        assertEquals("GID 1234 not found", response?.error?.message)
    }

    @Test
    fun aria2DownloadStatus_parsesAllKnownStatuses() {
        assertEquals(Aria2DownloadStatus.ACTIVE, Aria2DownloadStatus.fromString("active"))
        assertEquals(Aria2DownloadStatus.WAITING, Aria2DownloadStatus.fromString("waiting"))
        assertEquals(Aria2DownloadStatus.PAUSED, Aria2DownloadStatus.fromString("paused"))
        assertEquals(Aria2DownloadStatus.ERROR, Aria2DownloadStatus.fromString("error"))
        assertEquals(Aria2DownloadStatus.COMPLETE, Aria2DownloadStatus.fromString("complete"))
        assertEquals(Aria2DownloadStatus.REMOVED, Aria2DownloadStatus.fromString("removed"))
        assertEquals(Aria2DownloadStatus.WAITING, Aria2DownloadStatus.fromString("unknown_status"))
    }
}
