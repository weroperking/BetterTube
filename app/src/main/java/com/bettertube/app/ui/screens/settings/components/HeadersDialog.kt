package com.bettertube.app.ui.screens.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bettertube.app.ui.theme.BetterTubeTheme
import com.bettertube.app.ui.theme.BrandPrimary
import com.bettertube.app.ui.theme.TextSecondary

data class HeaderEntry(var key: String, var value: String)

@Composable
fun HeadersDialog(
    initialHeaders: Map<String, String>,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit,
    onValidationError: (String) -> Unit
) {
    val headersList = remember {
        mutableStateListOf<HeaderEntry>().apply {
            if (initialHeaders.isEmpty()) {
                add(HeaderEntry("", ""))
            } else {
                initialHeaders.forEach { (k, v) -> add(HeaderEntry(k, v)) }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Custom HTTP Headers",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
            ) {
                Text(
                    text = "Specify custom request headers applied to all aria2 downloads.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(headersList) { index, entry ->
                        var key by remember(entry) { mutableStateOf(entry.key) }
                        var value by remember(entry) { mutableStateOf(entry.value) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedTextField(
                                value = key,
                                onValueChange = {
                                    key = it
                                    headersList[index] = HeaderEntry(it, value)
                                },
                                label = { Text("Header") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("header_key_$index")
                            )

                            OutlinedTextField(
                                value = value,
                                onValueChange = {
                                    value = it
                                    headersList[index] = HeaderEntry(key, it)
                                },
                                label = { Text("Value") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("header_val_$index")
                            )

                            IconButton(
                                onClick = {
                                    if (headersList.size > 1) {
                                        headersList.removeAt(index)
                                    } else {
                                        headersList[0] = HeaderEntry("", "")
                                    }
                                },
                                modifier = Modifier.testTag("header_delete_$index")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { headersList.add(HeaderEntry("", "")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("header_add_row_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Add Header")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val filtered = headersList.filter { it.key.isNotBlank() || it.value.isNotBlank() }
                    for (entry in filtered) {
                        if (entry.key.isBlank() || entry.value.isBlank()) {
                            onValidationError("Invalid header")
                            return@Button
                        }
                    }
                    val resultMap = filtered.associate { it.key.trim() to it.value.trim() }
                    onSave(resultMap)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("headers_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("headers_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun HeadersDialogPreview() {
    BetterTubeTheme {
        HeadersDialog(
            initialHeaders = mapOf("User-Agent" to "Mozilla/5.0"),
            onDismiss = {},
            onSave = {},
            onValidationError = {}
        )
    }
}
