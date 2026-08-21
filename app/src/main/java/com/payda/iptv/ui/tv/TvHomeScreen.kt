package com.payda.iptv.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun TvHomeScreen(
    channelCount: Int,
    moviesEnabled: Boolean,
    moviesSubtitle: String,
    accountSummary: String?,
    onOpenLiveTv: () -> Unit,
    onOpenMovies: () -> Unit,
    onOpenAccount: () -> Unit,
    onChangePlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val liveFocusRequester = remember { FocusRequester() }
    val nowText = remember {
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm · dd/MM/yyyy"))
    }

    LaunchedEffect(Unit) {
        liveFocusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackground)
            .padding(horizontal = 56.dp, vertical = 42.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "PayDa IPTV",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = nowText,
                color = Color(0xFFCBD5E1),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(modifier = Modifier.height(56.dp))
        if (!accountSummary.isNullOrBlank()) {
            Text(
                text = accountSummary,
                color = Color(0xFFCBD5E1),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            TvHomeCard(
                title = "TV EN DIRECTO",
                subtitle = "$channelCount canales",
                enabled = true,
                onClick = onOpenLiveTv,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(liveFocusRequester),
            )
            TvHomeCard(
                title = "PELICULAS",
                subtitle = moviesSubtitle,
                enabled = moviesEnabled,
                onClick = onOpenMovies,
                modifier = Modifier.weight(1f),
            )
            TvHomeCard(
                title = "SERIES",
                subtitle = "Proximamente",
                enabled = false,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TvFocusableButton(
                text = "Cuenta",
                onClick = onOpenAccount,
                modifier = Modifier.width(180.dp),
            )
            TvFocusableButton(
                text = "Playlists",
                onClick = onChangePlaylist,
                modifier = Modifier.width(180.dp),
            )
        }
    }
}

@Composable
private fun TvHomeCard(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvFocusableCard(
        onClick = onClick,
        modifier = modifier
            .height(210.dp),
        enabled = enabled,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                color = if (enabled) Color.White else TvDisabledText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = if (enabled) Color(0xFFCBD5E1) else TvDisabledText,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

internal val TvBackground = Color(0xFF0B0F12)
