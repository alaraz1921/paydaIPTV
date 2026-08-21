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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.payda.iptv.data.PlaylistSourceType
import com.payda.iptv.data.XtreamAccountInfo
import com.payda.iptv.ui.theme.PayDaBackground
import com.payda.iptv.ui.theme.PayDaButton
import com.payda.iptv.ui.theme.PayDaCard
import com.payda.iptv.ui.theme.PayDaSurface
import com.payda.iptv.ui.theme.PayDaTextPrimary
import com.payda.iptv.ui.theme.PayDaTextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AccountScreen(
    sourceType: PlaylistSourceType,
    accountInfo: XtreamAccountInfo?,
    isTv: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PayDaBackground)
            .padding(if (isTv) 42.dp else 18.dp),
    ) {
        Text(
            text = "Cuenta",
            color = PayDaTextPrimary,
            style = if (isTv) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(18.dp))
        if (sourceType != PlaylistSourceType.XTREAM || accountInfo == null) {
            PayDaCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
            ) {
                Text(
                    text = "Esta fuente no proporciona informacion de cuenta.",
                    color = PayDaTextSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            PayDaCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AccountStatusBadge(accountInfo.status)
                    AccountRow("Usuario", accountInfo.username.orEmpty().ifBlank { "No disponible" })
                    AccountRow("Estado", translateStatus(accountInfo.status))
                    AccountRow("Vencimiento", formatXtreamDate(accountInfo.expiresAtEpochSeconds) ?: "Sin informacion")
                    AccountRow("Cuenta de prueba", accountInfo.isTrial?.let { if (it) "Si" else "No" } ?: "No disponible")
                    AccountRow("Conexiones activas", accountInfo.activeConnections?.toString() ?: "No disponible")
                    AccountRow("Maximo conexiones", accountInfo.maxConnections?.toString() ?: "No disponible")
                    AccountRow("Creada", formatXtreamDate(accountInfo.createdAtEpochSeconds) ?: "No disponible")
                    if (!accountInfo.server.isNullOrBlank()) {
                        AccountRow("Servidor", accountInfo.server)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        PayDaButton(text = "Volver", onClick = onBack)
    }
}

@Composable
private fun AccountRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = PayDaTextSecondary, style = MaterialTheme.typography.bodyLarge)
        Text(text = value, color = PayDaTextPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AccountStatusBadge(status: String?) {
    Surface(
        color = PayDaSurface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
    ) {
        Text(
            text = translateStatus(status),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = PayDaTextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

internal fun translateStatus(status: String?): String {
    return when (status?.trim()?.lowercase()) {
        "active" -> "Activa"
        "expired" -> "Expirada"
        "disabled", "inactive" -> "Deshabilitada"
        "banned" -> "Bloqueada"
        null, "" -> "Sin informacion"
        else -> status
    }
}

internal fun formatXtreamDate(epochSeconds: Long?): String? {
    val value = epochSeconds?.takeIf { it > 0 } ?: return null
    return Instant.ofEpochSecond(value)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "ES")))
}
