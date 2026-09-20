package com.bettertube.app.ui.screens.downloads.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertube.app.domain.model.Aria2Download
import com.bettertube.app.domain.model.Aria2DownloadStatus
import com.bettertube.app.ui.theme.BetterTubeTheme
import com.bettertube.app.ui.theme.BgSecondary
import com.bettertube.app.ui.theme.BrandPrimary
import com.bettertube.app.ui.theme.SurfaceDark
import com.bettertube.app.ui.theme.TextSecondary
import com.bettertube.app.utils.FormatUtils

@Composable
fun Aria2TaskCard(
    download: Aria2Download,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) SurfaceDark else BgSecondary

    val progress = if (download.totalBytes > 0) {
        (download.completedBytes.toFloat() / download.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = modifier
            .fillMaxWidth()
            .testTag("aria2_task_card_${download.gid}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Type badge + Name + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge: BitTorrent or Direct
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (download.isTorrent) BrandPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (download.isTorrent) Icons.Default.CloudDownload else Icons.Default.Download,
                                contentDescription = null,
                                tint = if (download.isTorrent) BrandPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (download.isTorrent) "TORRENT" else "ARIA2",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (download.isTorrent) BrandPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        text = download.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Status text
                Text(
                    text = download.status.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = when (download.status) {
                        Aria2DownloadStatus.ACTIVE -> Color(0xFF4CAF50)
                        Aria2DownloadStatus.PAUSED -> BrandPrimary
                        Aria2DownloadStatus.COMPLETE -> Color(0xFF2196F3)
                        Aria2DownloadStatus.ERROR -> Color(0xFFF44336)
                        Aria2DownloadStatus.WAITING -> TextSecondary
                        Aria2DownloadStatus.REMOVED -> TextSecondary
                    },
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            if (download.status == Aria2DownloadStatus.ACTIVE || download.status == Aria2DownloadStatus.PAUSED) {
                if (download.totalBytes > 0) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = BrandPrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = BrandPrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Stats row & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val sizeText = if (download.totalBytes > 0) {
                        "${FormatUtils.formatBytes(download.completedBytes)} / ${FormatUtils.formatBytes(download.totalBytes)}"
                    } else {
                        FormatUtils.formatBytes(download.completedBytes)
                    }

                    Text(
                        text = sizeText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextSecondary
                    )

                    if (download.status == Aria2DownloadStatus.ACTIVE) {
                        val speedText = if (download.downloadSpeed > 0) FormatUtils.formatSpeed(download.downloadSpeed) else "Connecting..."
                        val peersText = if (download.isTorrent) " · ${download.seeders} seeders" else ""
                        Text(
                            text = "$speedText$peersText",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = BrandPrimary
                        )
                    } else if (download.isTorrent && download.status == Aria2DownloadStatus.WAITING) {
                        Text(
                            text = "${download.seeders} seeders",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary
                        )
                    }
                }

                // Action controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (download.status) {
                        Aria2DownloadStatus.ACTIVE -> {
                            IconButton(
                                onClick = { onPause(download.gid) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("aria2_pause_${download.gid}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Pause aria2 download",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Aria2DownloadStatus.PAUSED -> {
                            IconButton(
                                onClick = { onResume(download.gid) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("aria2_resume_${download.gid}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Resume aria2 download",
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        else -> {}
                    }

                    IconButton(
                        onClick = { onRemove(download.gid) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("aria2_remove_${download.gid}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove aria2 download",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (download.errorMessage != null && download.status == Aria2DownloadStatus.ERROR) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = download.errorMessage,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Aria2TaskCardPreview() {
    BetterTubeTheme {
        Aria2TaskCard(
            download = Aria2Download(
                gid = "123456",
                name = "ubuntu-22.04-desktop-amd64.iso",
                status = Aria2DownloadStatus.ACTIVE,
                totalBytes = 3654000000L,
                completedBytes = 1200000000L,
                downloadSpeed = 4500000L,
                uploadSpeed = 120000L,
                connections = 32,
                seeders = 142,
                isTorrent = true,
                isMetalink = false,
                errorMessage = null
            ),
            onPause = {},
            onResume = {},
            onRemove = {}
        )
    }
}
