package com.payda.iptv.ui.tv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun TvHomeScreen(
    channelCount: Int,
    onOpenLiveTv: () -> Unit,
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
                subtitle = "Proximamente",
                enabled = false,
                onClick = {},
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
            TvSecondaryAction(
                text = "Playlist",
                onClick = onChangePlaylist,
            )
            TvSecondaryAction(
                text = "Configuracion",
                onClick = {},
                enabled = false,
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
    var hasFocus by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .height(210.dp)
            .onFocusChanged { hasFocus = it.hasFocus }
            .focusable(enabled)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = when {
            !enabled -> Color(0xFF141A20)
            hasFocus -> Color(0xFF26374A)
            else -> Color(0xFF1A232B)
        },
        border = BorderStroke(
            width = if (hasFocus) 3.dp else 1.dp,
            color = if (hasFocus) Color.White else Color(0xFF2F3B46),
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                color = if (enabled) Color.White else Color(0xFF64748B),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = if (enabled) Color(0xFFCBD5E1) else Color(0xFF64748B),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun TvSecondaryAction(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    var hasFocus by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .width(180.dp)
            .height(56.dp)
            .onFocusChanged { hasFocus = it.hasFocus }
            .focusable(enabled)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (hasFocus) Color(0xFF26374A) else Color(0xFF151C22),
        border = BorderStroke(
            width = if (hasFocus) 2.dp else 1.dp,
            color = if (hasFocus) Color.White else Color(0xFF2F3B46),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                color = if (enabled) Color.White else Color(0xFF64748B),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

internal val TvBackground = Color(0xFF0B0F12)
