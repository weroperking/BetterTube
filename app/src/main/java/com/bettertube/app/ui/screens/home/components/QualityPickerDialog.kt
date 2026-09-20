package com.bettertube.app.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bettertube.app.domain.model.MediaFormat
import com.bettertube.app.domain.model.MediaMetadata
import com.bettertube.app.ui.components.pressScale
import com.bettertube.app.ui.theme.BetterTubeTheme
import com.bettertube.app.ui.theme.BrandPrimary
import com.bettertube.app.ui.theme.TextSecondary
import com.bettertube.app.ui.utils.rememberHaptics
import com.bettertube.app.utils.FormatUtils
import com.example.R

@Composable
fun QualityPickerDialog(
    metadata: MediaMetadata,
    onDismiss: () -> Unit,
    onDownload: (MediaFormat) -> Unit
) {
    val haptics = rememberHaptics()
    var isDownloading by remember { mutableStateOf(false) }

    val videoFormats = remember(metadata) {
        metadata.formats.filter { it.isVideo && it.note.isNotBlank() }
            .ifEmpty { metadata.formats.filter { it.isVideo } }
    }
    val audioFormats = remember(metadata) {
        metadata.formats.filter { it.isAudio }
    }

    var selectedTabIndex by remember {
        mutableIntStateOf(if (videoFormats.isNotEmpty()) 0 else 1)
    }

    var selectedFormat by remember(metadata) {
        mutableStateOf(
            if (selectedTabIndex == 0 && videoFormats.isNotEmpty()) {
                videoFormats.first()
            } else if (audioFormats.isNotEmpty()) {
                audioFormats.first()
            } else {
                metadata.formats.firstOrNull()
            }
        )
    }

    AlertDialog(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        title = {
            Text(
                text = stringResource(R.string.select_format_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quality_picker_dialog")
            ) {
                // Header with video info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (metadata.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = metadata.thumbnailUrl,
                            contentDescription = metadata.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 80.dp, height = 50.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = metadata.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (metadata.uploader.isNotBlank()) {
                            Text(
                                text = metadata.uploader,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Tabs: Video / Audio
                if (videoFormats.isNotEmpty() && audioFormats.isNotEmpty()) {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = BrandPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = {
                                selectedTabIndex = 0
                                if (videoFormats.isNotEmpty()) selectedFormat = videoFormats.first()
                            },
                            text = { Text("Video") },
                            icon = { Icon(Icons.Default.Videocam, contentDescription = "Video", modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = {
                                selectedTabIndex = 1
                                if (audioFormats.isNotEmpty()) selectedFormat = audioFormats.first()
                            },
                            text = { Text("Audio") },
                            icon = { Icon(Icons.Default.Audiotrack, contentDescription = "Audio", modifier = Modifier.size(18.dp)) }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                val currentList = if (selectedTabIndex == 0 && videoFormats.isNotEmpty()) videoFormats else audioFormats

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(currentList, key = { it.formatId }) { format ->
                        val isSelected = selectedFormat?.formatId == format.formatId
                        Card(
                            onClick = {
                                selectedFormat = format
                                haptics.selectionChanged()
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) BrandPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("quality_option_${format.formatId}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val label = format.note.ifBlank { format.resolution ?: format.extension }
                                    Text(
                                        text = label.uppercase(),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${format.extension.uppercase()}${if (format.fileSizeBytes != null) " · " + FormatUtils.formatBytes(format.fileSizeBytes) else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = BrandPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isDownloading && selectedFormat != null) {
                        isDownloading = true
                        haptics.confirm()
                        onDownload(selectedFormat!!)
                    }
                },
                enabled = !isDownloading && selectedFormat != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .pressScale()
                    .testTag("quality_picker_download_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.download))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDownloading,
                modifier = Modifier.testTag("quality_picker_cancel_button")
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun QualityPickerDialogPreview() {
    BetterTubeTheme {
        QualityPickerDialog(
            metadata = MediaMetadata(
                id = "demo",
                title = "Demo Video Title For Testing",
                webpageUrl = "https://youtube.com/watch?v=123",
                uploader = "Creator Name",
                thumbnailUrl = "",
                durationSeconds = 180,
                extractor = "youtube",
                formats = listOf(
                    MediaFormat(
                        formatId = "1080p",
                        extension = "mp4",
                        resolution = "1920x1080",
                        audioBitrate = null,
                        videoBitrate = 4000,
                        fileSizeBytes = 52428800L,
                        note = "1080p HD",
                        isVideo = true,
                        isAudio = false
                    ),
                    MediaFormat(
                        formatId = "720p",
                        extension = "mp4",
                        resolution = "1280x720",
                        audioBitrate = null,
                        videoBitrate = 2000,
                        fileSizeBytes = 26214400L,
                        note = "720p",
                        isVideo = true,
                        isAudio = false
                    ),
                    MediaFormat(
                        formatId = "mp3",
                        extension = "mp3",
                        resolution = null,
                        audioBitrate = 320,
                        videoBitrate = null,
                        fileSizeBytes = 5242880L,
                        note = "320kbps",
                        isVideo = false,
                        isAudio = true
                    )
                )
            ),
            onDismiss = {},
            onDownload = {}
        )
    }
}
