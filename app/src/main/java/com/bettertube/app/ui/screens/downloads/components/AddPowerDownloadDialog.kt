package com.bettertube.app

import android.util.Log.ui.screens.downloads.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bettertube.app.ui.theme.BetterTubeTheme
import com.bettertube.app.ui.theme.BrandPrimary
import com.bettertube.app.ui.theme.TextSecondary

@Composable
fun AddPowerDownloadDialog(
    onDismiss: () -> Unit,
    onAddMagnet: (String) -> Unit,
    onAddHttpUrl: (String) -> Unit,
    onAddTorrentFile: (ByteArray) -> Unit,
    onAddMetalinkFile: (ByteArray) -> Unit
) {
    val context = LocalContext.current
    var urlText by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isMetalinkFile by remember { mutableStateOf(false) }

    val torrentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    selectedFileBytes = bytes
                    selectedFileName = uri.lastPathSegment ?: "Selected Torrent"
                    isMetalinkFile = false
                }
            } catch (e: Exception) {
                Log.w("AddPowerDownloadDialog", "Failed to read torrent file", e)
            }
        }
    }

    val metalinkPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    selectedFileBytes = bytes
                    selectedFileName = uri.lastPathSegment ?: "Selected Metalink"
                    isMetalinkFile = true
                }
            } catch (e: Exception) {
                Log.w("AddPowerDownloadDialog", "Failed to read metalink file", e)
            }
        }
    }

    val trimmedUrl = urlText.trim()
    val isMagnet = trimmedUrl.startsWith("magnet:?", ignoreCase = true)
    val isHttp = trimmedUrl.startsWith("http://", ignoreCase = true) || trimmedUrl.startsWith("https://", ignoreCase = true)
    val canConfirm = isMagnet || isHttp || (selectedFileBytes != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Torrent / Magnet / URL",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Paste a BitTorrent magnet URI, direct download URL, or choose a file to queue in aria2.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = urlText,
                    onValueChange = {
                        urlText = it
                        if (it.isNotBlank()) {
                            selectedFileBytes = null
                            selectedFileName = null
                        }
                    },
                    label = { Text("Magnet link or HTTP(S) URL") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = TextSecondary)
                    },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("power_download_url_input")
                )

                if (selectedFileName != null) {
                    Text(
                        text = "Selected file: $selectedFileName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandPrimary,
                        modifier = Modifier.testTag("selected_file_label")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { torrentPicker.launch(arrayOf("*/*", "application/x-bittorrent")) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_torrent_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(".torrent", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = { metalinkPicker.launch(arrayOf("*/*", "application/metalink4+xml", "application/metalink+xml")) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_metalink_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(".metalink", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedFileBytes != null) {
                        if (isMetalinkFile) {
                            onAddMetalinkFile(selectedFileBytes!!)
                        } else {
                            onAddTorrentFile(selectedFileBytes!!)
                        }
                    } else if (isMagnet) {
                        onAddMagnet(trimmedUrl)
                    } else if (isHttp) {
                        onAddHttpUrl(trimmedUrl)
                    }
                    onDismiss()
                },
                enabled = canConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("power_download_confirm_button")
            ) {
                Text("Add Download")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("power_download_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AddPowerDownloadDialogPreview() {
    BetterTubeTheme {
        AddPowerDownloadDialog(
            onDismiss = {},
            onAddMagnet = {},
            onAddHttpUrl = {},
            onAddTorrentFile = {},
            onAddMetalinkFile = {}
        )
    }
}
