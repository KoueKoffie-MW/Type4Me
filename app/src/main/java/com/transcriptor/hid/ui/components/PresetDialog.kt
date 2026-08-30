package com.transcriptor.hid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.transcriptor.hid.R
import com.transcriptor.hid.ai.PromptPreset
import com.transcriptor.hid.ui.theme.ElectricViolet
import com.transcriptor.hid.ui.theme.StatusDisconnected

@Composable
fun PresetDialog(
    preset: PromptPreset?,
    onDismiss: () -> Unit,
    onSave: (title: String, prompt: String, description: String) -> Unit,
    onDelete: ((PromptPreset) -> Unit)? = null
) {
    var title by remember(preset) { mutableStateOf(preset?.title ?: "") }
    var prompt by remember(preset) { mutableStateOf(preset?.systemPrompt ?: "") }
    var description by remember(preset) { mutableStateOf(preset?.description ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val isEditingCustom = preset != null && !preset.isBuiltIn

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = if (isEditingCustom) stringResource(R.string.dialog_edit_preset_title) else stringResource(R.string.dialog_create_preset_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorText = null
                    },
                    label = { Text(stringResource(R.string.preset_field_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = prompt,
                    onValueChange = {
                        prompt = it
                        errorText = null
                    },
                    label = { Text(stringResource(R.string.preset_field_prompt)) },
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.preset_field_description)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || prompt.isBlank()) {
                        errorText = "Title and system prompt cannot be blank."
                    } else {
                        onSave(title, prompt, description)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isEditingCustom && onDelete != null && preset != null) {
                    TextButton(
                        onClick = { onDelete(preset) },
                        colors = ButtonDefaults.textButtonColors(contentColor = StatusDisconnected)
                    ) {
                        Text(stringResource(R.string.btn_delete))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        }
    )
}
