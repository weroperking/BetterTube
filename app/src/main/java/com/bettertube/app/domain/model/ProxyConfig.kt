package com.bettertube.app.domain.model

data class ProxyConfig(
    val enabled: Boolean = false,
    val host: String = "",
    val port: Int = 8080,
    val username: String? = null,
    val password: String? = null
)
