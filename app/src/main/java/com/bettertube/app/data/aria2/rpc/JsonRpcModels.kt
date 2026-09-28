package com.bettertube.app.data.aria2.rpc

import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: String = UUID.randomUUID().toString(),
    val method: String,
    val params: List<Any> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JsonRpcResponse<T>(
    val jsonrpc: String = "2.0",
    val id: String? = null,
    val result: T? = null,
    val error: JsonRpcError? = null
)

@JsonClass(generateAdapter = true)
data class JsonRpcError(
    val code: Int,
    val message: String
)

@JsonClass(generateAdapter = true)
data class Aria2Status(
    val gid: String,
    val status: String,
    val totalLength: String? = "0",
    val completedLength: String? = "0",
    val downloadSpeed: String? = "0",
    val uploadSpeed: String? = "0",
    val connections: String? = "0",
    val numSeeders: String? = null,
    val seeder: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val dir: String? = null,
    val files: List<Aria2File>? = null,
    val bittorrent: Aria2BitTorrent? = null
)

@JsonClass(generateAdapter = true)
data class Aria2File(
    val index: String? = null,
    val path: String? = null,
    val length: String? = "0",
    val completedLength: String? = "0",
    val selected: String? = null,
    val uris: List<Aria2Uri>? = null
)

@JsonClass(generateAdapter = true)
data class Aria2Uri(
    val uri: String? = null,
    val status: String? = null
)

@JsonClass(generateAdapter = true)
data class Aria2BitTorrent(
    val mode: String? = null,
    val info: Aria2BitTorrentInfo? = null
)

@JsonClass(generateAdapter = true)
data class Aria2BitTorrentInfo(
    val name: String? = null
)

@JsonClass(generateAdapter = true)
data class Aria2GlobalStat(
    val downloadSpeed: String? = "0",
    val uploadSpeed: String? = "0",
    val numActive: String? = "0",
    val numWaiting: String? = "0",
    val numStopped: String? = "0",
    val numStoppedTotal: String? = "0"
)

@JsonClass(generateAdapter = true)
data class Aria2Version(
    val version: String,
    val enabledFeatures: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class Aria2SessionInfo(
    val sessionId: String
)
