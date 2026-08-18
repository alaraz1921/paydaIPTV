package com.payda.iptv.ui.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun PlaylistScreen(
    playlistUrl: String,
    onPlaylistUrlChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onLoadPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }
    var clipboardMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "PayDa IPTV",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = playlistUrl,
            onValueChange = onPlaylistUrlChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text("URL de lista M3U") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            enabled = !isLoading,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    onPlaylistUrlChange("")
                    clipboardMessage = null
                    focusRequester.requestFocus()
                },
                enabled = !isLoading && playlistUrl.isNotEmpty(),
            ) {
                Text("Borrar")
            }
            OutlinedButton(
                onClick = {
                    val clipboardText = clipboardManager.getText()?.text?.trim()
                    if (clipboardText.isNullOrBlank()) {
                        clipboardMessage = "El portapapeles no contiene una URL valida."
                    } else {
                        onPlaylistUrlChange(clipboardText)
                        clipboardMessage = "URL pegada. Pulsa Cargar lista para continuar."
                    }
                    focusRequester.requestFocus()
                },
                enabled = !isLoading,
            ) {
                Text("Pegar")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onLoadPlaylist,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && playlistUrl.isNotBlank(),
        ) {
            Text("Cargar lista")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (isLoading) {
            CircularProgressIndicator()
        }
        if (!clipboardMessage.isNullOrBlank()) {
            Text(
                text = clipboardMessage.orEmpty(),
                modifier = Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                modifier = Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
