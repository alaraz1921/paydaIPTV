package com.payda.iptv.ui.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.payda.iptv.data.PlaylistConfig
import com.payda.iptv.data.PlaylistConfigStatus
import com.payda.iptv.data.PlaylistSourceType
import com.payda.iptv.data.summary
import com.payda.iptv.ui.theme.PayDaBackground
import com.payda.iptv.ui.theme.PayDaButton
import com.payda.iptv.ui.theme.PayDaCard
import com.payda.iptv.ui.theme.PayDaTextDisabled
import com.payda.iptv.ui.theme.PayDaTextPrimary
import com.payda.iptv.ui.theme.PayDaTextSecondary

@Composable
fun PlaylistManagerScreen(
    configs: List<PlaylistConfig>,
    isTv: Boolean,
    onAdd: () -> Unit,
    onEdit: (PlaylistConfig) -> Unit,
    onActivate: (PlaylistConfig) -> Unit,
    onDelete: (PlaylistConfig) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<PlaylistConfig?>(null) }
    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PayDaBackground)
            .padding(if (isTv) 42.dp else 18.dp),
    ) {
        Text(
            text = "Playlists",
            color = PayDaTextPrimary,
            style = if (isTv) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        PayDaButton(text = "+ Anadir playlist", onClick = onAdd, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(configs, key = { it.id }) { config ->
                PlaylistConfigCard(
                    config = config,
                    onActivate = { onActivate(config) },
                    onEdit = { onEdit(config) },
                    onDelete = { pendingDelete = config },
                )
            }
        }
    }

    val configToDelete = pendingDelete
    if (configToDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar playlist") },
            text = { Text("¿Eliminar esta playlist?") },
            confirmButton = {
                PayDaButton(
                    text = "Eliminar",
                    onClick = {
                        pendingDelete = null
                        onDelete(configToDelete)
                    },
                )
            },
            dismissButton = {
                PayDaButton(text = "Cancelar", onClick = { pendingDelete = null })
            },
        )
    }
}

@Composable
private fun PlaylistConfigCard(
    config: PlaylistConfig,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    PayDaCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onActivate,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = config.displayName,
                        color = PayDaTextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (config.sourceType == PlaylistSourceType.XTREAM) "Xtream" else "M3U",
                        color = PayDaTextSecondary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = config.summary(),
                        color = PayDaTextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = if (config.isActive) "Activa" else statusText(config.status),
                    color = if (config.isActive) PayDaTextPrimary else PayDaTextDisabled,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PayDaButton(text = "Editar", onClick = onEdit, modifier = Modifier.weight(1f))
                PayDaButton(text = "Eliminar", onClick = onDelete, modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun statusText(status: PlaylistConfigStatus): String {
    return when (status) {
        PlaylistConfigStatus.UNKNOWN -> ""
        PlaylistConfigStatus.ACTIVE -> "Activa"
        PlaylistConfigStatus.EXPIRED -> "Expirada"
        PlaylistConfigStatus.AVAILABLE -> "Disponible"
        PlaylistConfigStatus.ERROR -> "Error"
    }
}
