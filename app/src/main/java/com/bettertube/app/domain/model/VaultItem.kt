package com.bettertube.app.domain.model

data class VaultItem(
    val id: String,
    val originalTaskId: String,
    val fileName: String,
    val encryptedPath: String,
    val originalPath: String?,
    val sizeBytes: Long,
    val mediaType: MediaType,
    val addedAtMillis: Long
)
