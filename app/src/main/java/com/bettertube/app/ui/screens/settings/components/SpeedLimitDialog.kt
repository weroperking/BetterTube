package com.bettertube.app.ui.screens.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bettertube.app.ui.theme.BetterTubeTheme
import com.bettertube.app.ui.theme.BrandPrimary

@Composable
fun SpeedLimitDialog(
    currentLimit: Long?,
    onDismiss: () -> Unit,
    onSave: (Long?) -> Unit
) {
    val presets = listOf(
        "Unlimited" to null,
        "512 KB/s" to 512L * 1024L,
        "1 MB/s" to 1024L * 1024L,
        "2 MB/s" to 2L * 1024L * 1024L,
        "5 MB/s" to 5L * 1024L * 1024L,
        "Custom" to -1L
    )

    var selectedOption by remember {
        val match = presets.firstOrNull { it.second == currentLimit }
        mutableStateOf(match?.first ?: if (currentLimit == null || currentLimit <= 0) "Unlimited" else "Custom")
    }

    var customKbInput by remember {
        val initialKb = if (currentLimit != null && currentLimit > 0) (currentLimit / 1024).toString() else ""
        mutableStateOf(initialKb)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Global Download Speed Limit",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                presets.forEach { (label, _) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = label }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedOption == label),
                            onClick = { selectedOption = label },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = BrandPrimary
                            ),
                            modifier = Modifier.testTag("speed_limit_radio_${label.lowercase().replace(" ", "_").replace("/", "_")}")
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                if (selectedOption == "Custom") {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customKbInput,
                        onValueChange = { customKbInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Speed limit in KB/s (0 for unlimited)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_speed_limit_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalLimit = when (selectedOption) {
                        "Unlimited" -> null
                        "512 KB/s" -> 512L * 1024L
                        "1 MB/s" -> 1024L * 1024L
                        "2 MB/s" -> 2L * 1024L * 1024L
                        "5 MB/s" -> 5L * 1024L * 1024L
                        "Custom" -> {
                            val kb = customKbInput.toLongOrNull() ?: 0L
                            if (kb <= 0L) null else kb * 1024L
                        }
                        else -> null
                    }
                    onSave(finalLimit)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("speed_limit_save_button")
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("speed_limit_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun SpeedLimitDialogPreview() {
    BetterTubeTheme {
        SpeedLimitDialog(
            currentLimit = 1048576L,
            onDismiss = {},
            onSave = {}
        )
    }
}
