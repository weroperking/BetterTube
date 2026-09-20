package com.bettertube.app.ui.screens.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bettertube.app.ui.theme.BetterTubeTheme
import com.bettertube.app.ui.theme.BrandPrimary
import java.util.Calendar

@Composable
fun ScheduleDaysDialog(
    currentDays: Set<Int>,
    onDismiss: () -> Unit,
    onSave: (Set<Int>) -> Unit
) {
    val dayLabels = listOf(
        Calendar.SUNDAY to "Sunday",
        Calendar.MONDAY to "Monday",
        Calendar.TUESDAY to "Tuesday",
        Calendar.WEDNESDAY to "Wednesday",
        Calendar.THURSDAY to "Thursday",
        Calendar.FRIDAY to "Friday",
        Calendar.SATURDAY to "Saturday"
    )

    var selectedDays by remember { mutableStateOf(currentDays) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Active Days of Week",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                dayLabels.forEach { (calDay, label) ->
                    val isChecked = selectedDays.contains(calDay)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedDays = if (isChecked) {
                                    if (selectedDays.size > 1) selectedDays - calDay else selectedDays
                                } else {
                                    selectedDays + calDay
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                selectedDays = if (checked) {
                                    selectedDays + calDay
                                } else {
                                    if (selectedDays.size > 1) selectedDays - calDay else selectedDays
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = BrandPrimary,
                                checkmarkColor = Color.Black
                            ),
                            modifier = Modifier.testTag("day_checkbox_${label.lowercase()}")
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedDays) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("schedule_days_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("schedule_days_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ScheduleDaysDialogPreview() {
    BetterTubeTheme {
        ScheduleDaysDialog(
            currentDays = setOf(1, 2, 3, 4, 5, 6, 7),
            onDismiss = {},
            onSave = {}
        )
    }
}
