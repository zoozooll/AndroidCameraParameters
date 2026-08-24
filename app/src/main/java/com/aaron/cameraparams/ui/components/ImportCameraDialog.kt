package com.aaron.cameraparams.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aaron.cameraparams.R
import com.aaron.cameraparams.ui.CameraViewModel
import com.aaron.cameraparams.ui.theme.MonospaceTypography
import kotlinx.coroutines.launch

/**
 * Dialog to register an external camera dump as a selectable camera entry.
 * The JSON can either be pasted as text or picked from a file via SAF.
 */
@Composable
fun ImportCameraDialog(viewModel: CameraViewModel, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var json by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }
    var busy by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            busy = true
            invalid = false
            scope.launch {
                viewModel.importCameraFromUri(uri, name) { success ->
                    busy = false
                    if (success) onDismiss() else invalid = true
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.import_camera_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.import_camera_name_label)) },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = json,
                    onValueChange = {
                        json = it
                        invalid = false
                    },
                    placeholder = { Text(stringResource(R.string.import_dialog_hint)) },
                    textStyle = MonospaceTypography,
                    isError = invalid,
                    supportingText = {
                        if (invalid) Text(stringResource(R.string.import_invalid_json))
                    },
                    minLines = 5,
                    maxLines = 10,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { filePicker.launch(arrayOf("*/*")) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.import_camera_from_file))
                }
                if (busy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = json.isNotBlank() && !busy,
                onClick = {
                    busy = true
                    invalid = false
                    scope.launch {
                        viewModel.importCameraFromText(name, json) { success ->
                            busy = false
                            if (success) onDismiss() else invalid = true
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
