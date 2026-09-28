package com.bettertube.app.domain.model

enum class ExtractionPreset {
    VIDEO_ORIGINAL,
    VIDEO_MP4,
    VIDEO_MKV,
    VIDEO_WEBM,
    AUDIO_MP3_128,
    AUDIO_MP3_320,
    AUDIO_M4A,
    AUDIO_FLAC,
    AUDIO_OPUS,
    AUDIO_WAV
}

fun ExtractionPreset.toYtDlpArgs(): List<String> {
    return when (this) {
        ExtractionPreset.VIDEO_ORIGINAL -> listOf("--merge-output-format", "mp4")
        ExtractionPreset.VIDEO_MP4 -> listOf("--recode-video", "mp4")
        ExtractionPreset.VIDEO_MKV -> listOf("--recode-video", "mkv")
        ExtractionPreset.VIDEO_WEBM -> listOf("--recode-video", "webm")
        ExtractionPreset.AUDIO_MP3_128 -> listOf("-x", "--audio-format", "mp3", "--audio-quality", "128K")
        ExtractionPreset.AUDIO_MP3_320 -> listOf("-x", "--audio-format", "mp3", "--audio-quality", "320K")
        ExtractionPreset.AUDIO_M4A -> listOf("-x", "--audio-format", "m4a")
        ExtractionPreset.AUDIO_FLAC -> listOf("-x", "--audio-format", "flac")
        ExtractionPreset.AUDIO_OPUS -> listOf("-x", "--audio-format", "opus")
        ExtractionPreset.AUDIO_WAV -> listOf("-x", "--audio-format", "wav")
    }
}
