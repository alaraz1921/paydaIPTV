package com.payda.iptv.ui.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.payda.iptv.data.PlaylistSourceType
import com.payda.iptv.ui.tv.TvBackground
import com.payda.iptv.ui.tv.TvFocusableButton
import com.payda.iptv.ui.theme.PayDaBackground
import com.payda.iptv.ui.theme.PayDaButton
import com.payda.iptv.ui.theme.PayDaError
import com.payda.iptv.ui.theme.PayDaTextFieldColors
import com.payda.iptv.ui.theme.PayDaTextPrimary
import com.payda.iptv.ui.theme.PayDaTextSecondary

@Composable
fun PlaylistScreen(
    sourceType: PlaylistSourceType,
    playlistUrl: String,
    epgUrl: String,
    xtreamServer: String,
    xtreamUsername: String,
    xtreamPassword: String,
    onSourceTypeChange: (PlaylistSourceType) -> Unit,
    onPlaylistUrlChange: (String) -> Unit,
    onEpgUrlChange: (String) -> Unit,
    onXtreamServerChange: (String) -> Unit,
    onXtreamUsernameChange: (String) -> Unit,
    onXtreamPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    loadingMessage: String?,
    errorMessage: String?,
    testPlaylistOptions: List<TestPlaylistOption>,
    testEpgOption: TestPlaylistOption?,
    testXtreamOption: TestXtreamOption?,
    onLoadPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    isTvStyle: Boolean = false,
    onBack: (() -> Unit)? = null,
) {
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val playlistFocusRequester = remember { FocusRequester() }
    val epgFocusRequester = remember { FocusRequester() }
    val xtreamServerFocusRequester = remember { FocusRequester() }
    var clipboardMessage by remember { mutableStateOf<String?>(null) }
    var playlistFieldHasFocus by remember { mutableStateOf(false) }
    var epgFieldHasFocus by remember { mutableStateOf(false) }
    var xtreamFieldHasFocus by remember { mutableStateOf(false) }
    val textFieldColors = PayDaTextFieldColors()
    BackHandler(enabled = onBack != null) {
        if (playlistFieldHasFocus || epgFieldHasFocus || xtreamFieldHasFocus) {
            keyboardController?.hide()
            focusManager.clearFocus()
        } else {
            onBack?.invoke()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isTvStyle) TvBackground else PayDaBackground)
            .verticalScroll(rememberScrollState())
            .padding(if (isTvStyle) 48.dp else 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "PayDa IPTV",
            color = PayDaTextPrimary,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Tipo de fuente",
            color = PayDaTextSecondary,
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlaylistActionButton(
                text = if (sourceType == PlaylistSourceType.M3U) "Lista M3U *" else "Lista M3U",
                isTvStyle = isTvStyle,
                onClick = { onSourceTypeChange(PlaylistSourceType.M3U) },
                enabled = !isLoading,
            )
            PlaylistActionButton(
                text = if (sourceType == PlaylistSourceType.XTREAM) "Xtream Codes *" else "Xtream Codes",
                isTvStyle = isTvStyle,
                enabled = !isLoading,
                onClick = { onSourceTypeChange(PlaylistSourceType.XTREAM) },
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        if (sourceType == PlaylistSourceType.M3U) {
            OutlinedTextField(
                value = playlistUrl,
                onValueChange = onPlaylistUrlChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(playlistFocusRequester)
                    .onFocusChanged { playlistFieldHasFocus = it.hasFocus },
                label = { Text("URL de lista M3U") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                enabled = !isLoading,
                colors = textFieldColors,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlaylistActionButton(
                    text = "Borrar",
                    isTvStyle = isTvStyle,
                    onClick = {
                        onPlaylistUrlChange("")
                        clipboardMessage = null
                        if (!isTvStyle) {
                            playlistFocusRequester.requestFocus()
                        }
                    },
                    enabled = !isLoading && playlistUrl.isNotEmpty(),
                )
                PlaylistActionButton(
                    text = "Pegar",
                    isTvStyle = isTvStyle,
                    onClick = {
                        val clipboardText = clipboardManager.getText()?.text?.trim()
                        if (clipboardText.isNullOrBlank()) {
                            clipboardMessage = "El portapapeles no contiene una URL valida."
                        } else {
                            onPlaylistUrlChange(clipboardText)
                            clipboardMessage = "URL pegada. Pulsa Cargar lista para continuar."
                        }
                        if (!isTvStyle) {
                            playlistFocusRequester.requestFocus()
                        }
                    },
                    enabled = !isLoading,
                )
            }
            if (testPlaylistOptions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    testPlaylistOptions.forEach { option ->
                        PlaylistActionButton(
                            text = option.label,
                            isTvStyle = isTvStyle,
                            onClick = {
                                onPlaylistUrlChange(option.url)
                                clipboardMessage = "${option.label} preparada. Pulsa Cargar lista."
                                if (!isTvStyle) {
                                    playlistFocusRequester.requestFocus()
                                }
                            },
                            enabled = !isLoading,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            OutlinedTextField(
                value = epgUrl,
                onValueChange = onEpgUrlChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(epgFocusRequester)
                    .onFocusChanged { epgFieldHasFocus = it.hasFocus },
                label = { Text("URL EPG / XMLTV") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                enabled = !isLoading,
                colors = textFieldColors,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlaylistActionButton(
                    text = "Borrar EPG",
                    isTvStyle = isTvStyle,
                    onClick = {
                        onEpgUrlChange("")
                        clipboardMessage = null
                        if (!isTvStyle) {
                            epgFocusRequester.requestFocus()
                        }
                    },
                    enabled = !isLoading && epgUrl.isNotEmpty(),
                )
                PlaylistActionButton(
                    text = "Pegar EPG",
                    isTvStyle = isTvStyle,
                    onClick = {
                        val clipboardText = clipboardManager.getText()?.text?.trim()
                        if (clipboardText.isNullOrBlank()) {
                            clipboardMessage = "El portapapeles no contiene una URL EPG valida."
                        } else {
                            onEpgUrlChange(clipboardText)
                            clipboardMessage = "URL EPG pegada."
                        }
                        if (!isTvStyle) {
                            epgFocusRequester.requestFocus()
                        }
                    },
                    enabled = !isLoading,
                )
                if (testEpgOption != null) {
                    PlaylistActionButton(
                        text = testEpgOption.label,
                        isTvStyle = isTvStyle,
                        onClick = {
                            onEpgUrlChange(testEpgOption.url)
                            clipboardMessage = "${testEpgOption.label} preparada."
                            if (!isTvStyle) {
                                epgFocusRequester.requestFocus()
                            }
                        },
                        enabled = !isLoading,
                    )
                }
            }
        } else {
            OutlinedTextField(
                value = xtreamServer,
                onValueChange = onXtreamServerChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(xtreamServerFocusRequester)
                    .onFocusChanged { xtreamFieldHasFocus = it.hasFocus },
                label = { Text("Servidor") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                enabled = !isLoading,
                colors = textFieldColors,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = xtreamUsername,
                onValueChange = onXtreamUsernameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { xtreamFieldHasFocus = it.hasFocus },
                label = { Text("Usuario") },
                singleLine = true,
                enabled = !isLoading,
                colors = textFieldColors,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = xtreamPassword,
                onValueChange = onXtreamPasswordChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { xtreamFieldHasFocus = it.hasFocus },
                label = { Text("Contrasena") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !isLoading,
                colors = textFieldColors,
            )
            if (testXtreamOption != null) {
                Spacer(modifier = Modifier.height(12.dp))
                PlaylistActionButton(
                    text = testXtreamOption.label,
                    isTvStyle = isTvStyle,
                    onClick = {
                        onXtreamServerChange(testXtreamOption.server)
                        onXtreamUsernameChange(testXtreamOption.username)
                        onXtreamPasswordChange(testXtreamOption.password)
                        clipboardMessage = "${testXtreamOption.label} preparado."
                    },
                    enabled = !isLoading,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        val canLoad = if (sourceType == PlaylistSourceType.M3U) {
            playlistUrl.isNotBlank()
        } else {
            xtreamServer.isNotBlank() && xtreamUsername.isNotBlank() && xtreamPassword.isNotBlank()
        }
        if (isTvStyle) {
            TvFocusableButton(
                text = if (sourceType == PlaylistSourceType.M3U) "Cargar lista" else "Conectar",
                onClick = onLoadPlaylist,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && canLoad,
            )
        } else {
            PayDaButton(
                text = if (sourceType == PlaylistSourceType.M3U) "Cargar lista" else "Conectar",
                onClick = onLoadPlaylist,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && canLoad,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (isLoading) {
            CircularProgressIndicator()
            if (!loadingMessage.isNullOrBlank()) {
                Text(
                    text = loadingMessage,
                    modifier = Modifier.padding(top = 12.dp),
                    color = PayDaTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (!clipboardMessage.isNullOrBlank()) {
            Text(
                text = clipboardMessage.orEmpty(),
                modifier = Modifier.padding(top = 16.dp),
                color = PayDaTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                modifier = Modifier.padding(top = 16.dp),
                color = PayDaError,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun PlaylistActionButton(
    text: String,
    isTvStyle: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    if (isTvStyle) {
        TvFocusableButton(
            text = text,
            onClick = onClick,
            enabled = enabled,
        )
    } else {
        PayDaButton(
            text = text,
            onClick = onClick,
            enabled = enabled,
        )
    }
}

data class TestPlaylistOption(
    val label: String,
    val url: String,
)

data class TestXtreamOption(
    val label: String,
    val server: String,
    val username: String,
    val password: String,
)
