package com.bettertube.app.ui.screens.settings.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertube.app.ui.theme.BrandPrimary
import com.bettertube.app.ui.theme.TextSecondary
import com.bettertube.app.utils.CrashLogger
import com.example.R
import java.io.File

@Composable
fun CrashLogsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var crashLogs by remember { mutableStateOf(CrashLogger.getCrashLogs(context)) }
    var selectedLogFile by remember { mutableStateOf<File?>(null) }

    if (selectedLogFile != null) {
        val file = selectedLogFile!!
        val content = remember(file) { CrashLogger.getCrashLogContent(file) }

        AlertDialog(
            onDismissRequest = { selectedLogFile = null },
            title = {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = content,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("Crash Log", content))
                        Toast.makeText(context, "Crash log copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("crash_log_copy_button")
                ) {
                    Text(text = stringResource(R.string.copy_to_clipboard))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedLogFile = null },
                    modifier = Modifier.testTag("crash_log_back_button")
                ) {
                    Text(text = stringResource(R.string.back))
                }
            },
            modifier = Modifier.testTag("crash_log_detail_dialog")
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.crash_logs_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (crashLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_crash_logs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .testTag("crash_logs_list")
                ) {
                    items(crashLogs, key = { it.name }) { file ->
                        Card(
                            onClick = { selectedLogFile = file },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("crash_log_item_${file.name}")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${file.length()} bytes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (crashLogs.isNotEmpty()) {
                TextButton(
                    onClick = {
                        CrashLogger.clearAllCrashLogs(context)
                        crashLogs = emptyList()
                        Toast.makeText(context, "Crash logs cleared", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("crash_logs_clear_button")
                ) {
                    Text(
                        text = stringResource(R.string.clear_all),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("crash_logs_close_button")
            ) {
                Text(text = stringResource(R.string.close))
            }
        },
        modifier = Modifier.testTag("crash_logs_dialog")
    )
}
