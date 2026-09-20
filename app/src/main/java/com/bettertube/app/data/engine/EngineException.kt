package com.bettertube.app.data.engine

sealed class EngineException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InitializationFailed(message: String = "Engine initialization failed", cause: Throwable? = null) : EngineException(message, cause)
    class NetworkUnavailable(message: String = "Network unavailable", cause: Throwable? = null) : EngineException(message, cause)
    class UnsupportedUrl(message: String = "Unsupported or invalid URL", cause: Throwable? = null) : EngineException(message, cause)
    class ExtractionFailed(message: String = "Extraction failed", cause: Throwable? = null) : EngineException(message, cause)
    class DownloadFailed(message: String = "Download failed", cause: Throwable? = null) : EngineException(message, cause)
    class Cancelled(message: String = "Download was cancelled", cause: Throwable? = null) : EngineException(message, cause)
    class StorageFull(message: String = "Storage full or unavailable", cause: Throwable? = null) : EngineException(message, cause)
}
