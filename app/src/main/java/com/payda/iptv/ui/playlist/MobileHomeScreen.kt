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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.payda.iptv.ui.theme.PayDaBackground
import com.payda.iptv.ui.theme.PayDaButton
import com.payda.iptv.ui.theme.PayDaCard
import com.payda.iptv.ui.theme.PayDaTextDisabled
import com.payda.iptv.ui.theme.PayDaTextPrimary
import com.payda.iptv.ui.theme.PayDaTextSecondary

@Composable
fun MobileHomeScreen(
    channelCount: Int,
    moviesEnabled: Boolean,
    moviesSubtitle: String,
    seriesEnabled: Boolean,
    seriesSubtitle: String,
    accountSummary: String?,
    onOpenLiveTv: () -> Unit,
    onOpenMovies: () -> Unit,
    onOpenSeries: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PayDaBackground)
            .padding(20.dp),
    ) {
        Text(
            text = "PayDa IPTV",
            color = PayDaTextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(28.dp))
        if (!accountSummary.isNullOrBlank()) {
            Text(
                text = accountSummary,
                color = PayDaTextSecondary,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(14.dp))
        }
        MobileHomeCard(
            title = "TV EN DIRECTO",
            subtitle = "$channelCount canales",
            onClick = onOpenLiveTv,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MobileHomeCard(
                title = "PELICULAS",
                subtitle = moviesSubtitle,
                enabled = moviesEnabled,
                onClick = onOpenMovies,
                modifier = Modifier
                    .weight(1f)
                    .height(116.dp),
            )
            MobileHomeCard(
                title = "SERIES",
                subtitle = seriesSubtitle,
                enabled = seriesEnabled,
                onClick = onOpenSeries,
                modifier = Modifier
                    .weight(1f)
                    .height(116.dp),
            )
        }
        Spacer(modifier = Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PayDaButton(
                text = "Cuenta",
                onClick = onOpenAccount,
                modifier = Modifier.weight(1f),
            )
            PayDaButton(
                text = "Playlists",
                onClick = onOpenPlaylist,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MobileHomeCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    PayDaCard(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                color = if (enabled) PayDaTextPrimary else PayDaTextDisabled,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = if (enabled) PayDaTextSecondary else PayDaTextDisabled,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
