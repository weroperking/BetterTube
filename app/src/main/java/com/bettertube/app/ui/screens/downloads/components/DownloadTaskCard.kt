package com.bettertube.app.ui.screens.downloads.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.bettertube.app.domain.model.DownloadStatus
import com.bettertube.app.domain.model.DownloadTask
import com.bettertube.app.domain.model.MediaType
import com.bettertube.app.ui.theme.BetterTubeTheme
import com.bettertube.app.ui.theme.BgSecondary
import com.bettertube.app.ui.theme.BrandPrimary
import com.bettertube.app.ui.theme.Divider
import com.bettertube.app.ui.theme.TextSecondary
import com.bettertube.app.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadTaskCard(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onCancel()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.error)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_cd),
                    tint = Color.White
                )
            }
        },
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("download_card_${task.id}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbnail placeholder
                        if (task.thumbnailUrl.isNotBlank()) {
                            AsyncImage(
                                model = task.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(width = 100.dp, height = 56.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(width = 100.dp, height = 56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BgSecondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Title and Metadata
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val metadataText: String
                            val metadataColor: Color
                            when (task.status) {
                                DownloadStatus.DOWNLOADING -> {
                                    metadataText = "${FormatUtils.formatBytes(task.downloadedBytes)} / ${FormatUtils.formatBytes(task.totalBytes)} · ${FormatUtils.formatSpeed(task.speedBytesPerSecond)}"
                                    metadataColor = TextSecondary
                                }
                                DownloadStatus.COMPLETE -> {
                                    metadataText = FormatUtils.formatBytes(task.totalBytes)
                                    metadataColor = TextSecondary
                                }
                                DownloadStatus.FAILED -> {
                                    metadataText = task.errorMessage ?: stringResource(R.string.unknown_error)
                                    metadataColor = MaterialTheme.colorScheme.error
                                }
                                DownloadStatus.PAUSED -> {
                                    metadataText = stringResource(R.string.paused_status, FormatUtils.formatBytes(task.downloadedBytes))
                                    metadataColor = TextSecondary
                                }
                                DownloadStatus.WAITING -> {
                                    metadataText = stringResource(R.string.waiting_in_queue)
                                    metadataColor = TextSecondary
                                }
                                DownloadStatus.EXTRACTING, DownloadStatus.MERGING, DownloadStatus.QUEUED, DownloadStatus.CANCELLED -> {
                                    metadataText = task.status.name.lowercase()
                                    metadataColor = TextSecondary
                                }
                            }

                            Text(
                                text = metadataText,
                                fontSize = 11.sp,
                                color = metadataColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Trailing Column
                        when (task.status) {
                            DownloadStatus.DOWNLOADING -> {
                                IconButton(onClick = onPause) {
                                    Icon(
                                        imageVector = Icons.Default.Pause,
                                        contentDescription = stringResource(R.string.pause_cd),
                                        tint = TextSecondary
                                    )
                                }
                            }
                            DownloadStatus.PAUSED -> {
                                IconButton(onClick = onResume) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.resume_cd),
                                        tint = BrandPrimary
                                    )
                                }
                            }
                            DownloadStatus.COMPLETE -> {
                                IconButton(onClick = onOpen) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.open_cd),
                                        tint = BrandPrimary
                                    )
                                }
                            }
                            DownloadStatus.FAILED -> {
                                IconButton(onClick = onRetry) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.retry_cd),
                                        tint = Color(0xFFFF9800) // Warning
                                    )
                                }
                            }
                            DownloadStatus.WAITING -> {
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (onMoveUp != null) {
                                        IconButton(
                                            onClick = onMoveUp,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = stringResource(R.string.move_up_cd),
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    if (onMoveDown != null) {
                                        IconButton(
                                            onClick = onMoveDown,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = stringResource(R.string.move_down_cd),
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                // Empty placeholder for alignment
                                Spacer(modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    // Progress indicators
                    if (task.status == DownloadStatus.DOWNLOADING) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val percentInt = (task.progressPercent * 100).toInt()
                        LinearProgressIndicator(
                            progress = { task.progressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .semantics { stateDescription = "$percentInt percent complete" },
                            color = BrandPrimary,
                            trackColor = Divider
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${(task.progressPercent * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = BrandPrimary
                            )
                            Text(
                                text = "ETA ${FormatUtils.formatEta(task.etaSeconds)}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    } else if (task.status == DownloadStatus.PAUSED || task.status == DownloadStatus.COMPLETE || task.status == DownloadStatus.FAILED) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { task.progressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = TextSecondary,
                            trackColor = Divider
                        )
                    }
                }
            }
        }
    )
}

// Previews for each status variant
@Preview(showBackground = true)
@Composable
fun DownloadTaskCardPreviewDownloading() {
    BetterTubeTheme {
        DownloadTaskCard(
            task = DownloadTask(
                id = "preview_1",
                url = "https://example.com/video1",
                title = "Big Buck Bunny 1080p 60fps Full HD Movie Sample",
                thumbnailUrl = "",
                formatId = "137",
                status = DownloadStatus.DOWNLOADING,
                progressPercent = 0.65f,
                downloadedBytes = 65000000L,
                totalBytes = 100000000L,
                speedBytesPerSecond = 2500000L,
                etaSeconds = 14L,
                outputFilePath = null,
                errorMessage = null,
                createdAtMillis = System.currentTimeMillis(),
                mediaType = MediaType.VIDEO
            ),
            onPause = {},
            onResume = {},
            onCancel = {},
            onRetry = {},
            onOpen = {},
            onMoveUp = null,
            onMoveDown = null
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DownloadTaskCardPreviewCompleted() {
    BetterTubeTheme {
        DownloadTaskCard(
            task = DownloadTask(
                id = "preview_2",
                url = "https://example.com/video2",
                title = "Tears of Steel 4K Open Source Film",
                thumbnailUrl = "",
                formatId = "137",
                status = DownloadStatus.COMPLETE,
                progressPercent = 1.0f,
                downloadedBytes = 150000000L,
                totalBytes = 150000000L,
                speedBytesPerSecond = 0L,
                etaSeconds = 0L,
                outputFilePath = "/path/to/file.mp4",
                errorMessage = null,
                createdAtMillis = System.currentTimeMillis(),
                mediaType = MediaType.VIDEO
            ),
            onPause = {},
            onResume = {},
            onCancel = {},
            onRetry = {},
            onOpen = {},
            onMoveUp = null,
            onMoveDown = null
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DownloadTaskCardPreviewFailed() {
    BetterTubeTheme {
        DownloadTaskCard(
            task = DownloadTask(
                id = "preview_3",
                url = "https://example.com/video3",
                title = "Network Timeout Error Video Stream",
                thumbnailUrl = "",
                formatId = "137",
                status = DownloadStatus.FAILED,
                progressPercent = 0.3f,
                downloadedBytes = 30000000L,
                totalBytes = 100000000L,
                speedBytesPerSecond = 0L,
                etaSeconds = 0L,
                outputFilePath = null,
                errorMessage = "Network timeout while fetching video segments",
                createdAtMillis = System.currentTimeMillis(),
                mediaType = MediaType.VIDEO
            ),
            onPause = {},
            onResume = {},
            onCancel = {},
            onRetry = {},
            onOpen = {},
            onMoveUp = null,
            onMoveDown = null
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DownloadTaskCardPreviewPaused() {
    BetterTubeTheme {
        DownloadTaskCard(
            task = DownloadTask(
                id = "preview_4",
                url = "https://example.com/video4",
                title = "Elephants Dream Animation Episode 1",
                thumbnailUrl = "",
                formatId = "137",
                status = DownloadStatus.PAUSED,
                progressPercent = 0.45f,
                downloadedBytes = 45000000L,
                totalBytes = 100000000L,
                speedBytesPerSecond = 0L,
                etaSeconds = 0L,
                outputFilePath = null,
                errorMessage = null,
                createdAtMillis = System.currentTimeMillis(),
                mediaType = MediaType.VIDEO
            ),
            onPause = {},
            onResume = {},
            onCancel = {},
            onRetry = {},
            onOpen = {},
            onMoveUp = null,
            onMoveDown = null
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DownloadTaskCardPreviewWaiting() {
    BetterTubeTheme {
        DownloadTaskCard(
            task = DownloadTask(
                id = "preview_5",
                url = "https://example.com/video5",
                title = "Cosmos Laundromat Trailer Waiting in Queue",
                thumbnailUrl = "",
                formatId = "137",
                status = DownloadStatus.WAITING,
                progressPercent = 0.0f,
                downloadedBytes = 0L,
                totalBytes = 0L,
                speedBytesPerSecond = 0L,
                etaSeconds = 0L,
                outputFilePath = null,
                errorMessage = null,
                createdAtMillis = System.currentTimeMillis(),
                mediaType = MediaType.VIDEO
            ),
            onPause = {},
            onResume = {},
            onCancel = {},
            onRetry = {},
            onOpen = {},
            onMoveUp = {},
            onMoveDown = {}
        )
    }
}
