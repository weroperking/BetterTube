package com.bettertube.app.ui.screens.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertube.app.ui.theme.BetterTubeTheme
import com.bettertube.app.ui.theme.BrandPrimary
import com.bettertube.app.ui.theme.TextSecondary

@Composable
fun ScheduleTimeDialog(
    startHour: Int,
    startMinute: Int,
    endHour: Int,
    endMinute: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Int, Int) -> Unit
) {
    var startH by remember { mutableStateOf(String.format("%02d", startHour)) }
    var startM by remember { mutableStateOf(String.format("%02d", startMinute)) }
    var endH by remember { mutableStateOf(String.format("%02d", endHour)) }
    var endM by remember { mutableStateOf(String.format("%02d", endMinute)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Active Download Hours",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Downloads will only run during this window (supports overnight windows like 22:00 to 06:00).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Start Time (24-hour format)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = startH,
                            onValueChange = { if (it.length <= 2) startH = it.filter { c -> c.isDigit() } },
                            label = { Text("Hour (0-23)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("schedule_start_hour_input")
                        )
                        Text(
                            text = ":",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        OutlinedTextField(
                            value = startM,
                            onValueChange = { if (it.length <= 2) startM = it.filter { c -> c.isDigit() } },
                            label = { Text("Min (0-59)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("schedule_start_min_input")
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "End Time (24-hour format)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = endH,
                            onValueChange = { if (it.length <= 2) endH = it.filter { c -> c.isDigit() } },
                            label = { Text("Hour (0-23)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("schedule_end_hour_input")
                        )
                        Text(
                            text = ":",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        OutlinedTextField(
                            value = endM,
                            onValueChange = { if (it.length <= 2) endM = it.filter { c -> c.isDigit() } },
                            label = { Text("Min (0-59)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("schedule_end_min_input")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sH = (startH.toIntOrNull() ?: 0).coerceIn(0, 23)
                    val sM = (startM.toIntOrNull() ?: 0).coerceIn(0, 59)
                    val eH = (endH.toIntOrNull() ?: 0).coerceIn(0, 23)
                    val eM = (endM.toIntOrNull() ?: 0).coerceIn(0, 59)
                    onSave(sH, sM, eH, eM)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("schedule_time_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("schedule_time_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ScheduleTimeDialogPreview() {
    BetterTubeTheme {
        ScheduleTimeDialog(
            startHour = 22,
            startMinute = 0,
            endHour = 6,
            endMinute = 0,
            onDismiss = {},
            onSave = { _, _, _, _ -> }
        )
    }
}
