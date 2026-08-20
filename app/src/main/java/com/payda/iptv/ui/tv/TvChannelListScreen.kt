package com.payda.iptv.ui.tv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.payda.iptv.data.Channel
import com.payda.iptv.data.stableFavoriteId
import com.payda.iptv.ui.playlist.AllCategoryName
import com.payda.iptv.ui.playlist.ChannelCategory
import com.payda.iptv.ui.playlist.FavoriteCategoryName
import com.payda.iptv.ui.playlist.buildChannelCategories
import com.payda.iptv.ui.playlist.filterChannels
import com.payda.iptv.ui.playlist.loadChannelLogo

@Composable
fun TvChannelListScreen(
    channels: List<Channel>,
    selectedCategoryName: String,
    searchQuery: String,
    favoriteChannelIds: Set<String>,
    lastSelectedChannelId: String?,
    onCategorySelected: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onChannelSelected: (Channel) -> Unit,
    onChangePlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories = remember(channels, favoriteChannelIds) {
        buildChannelCategories(channels, favoriteChannelIds)
    }
    val selectedCategory = remember(categories, selectedCategoryName) {
        categories.firstOrNull { it.name == selectedCategoryName } ?: categories.first()
    }
    val visibleChannels = remember(channels, selectedCategory, searchQuery, favoriteChannelIds) {
        filterChannels(
            channels = channels,
            selectedCategoryName = selectedCategory.name,
            searchQuery = searchQuery,
            favoriteChannelIds = favoriteChannelIds,
        )
    }
    val firstChannelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(selectedCategory.name, visibleChannels, lastSelectedChannelId) {
        if (visibleChannels.isNotEmpty()) {
            firstChannelFocusRequester.requestFocus()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101418))
            .padding(horizontal = 48.dp, vertical = 36.dp),
    ) {
        Text(
            text = "PayDa IPTV",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onChangePlaylist) {
            Text("Cambiar lista")
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Categorias",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(categories, key = { it.name }) { category ->
                TvCategoryChip(
                    category = category,
                    selected = category.name == selectedCategory.name,
                    onClick = { onCategorySelected(category.name) },
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar canal") },
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    TextButton(onClick = onClearSearch) {
                        Text("Limpiar")
                    }
                }
            },
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Canales (${visibleChannels.size})",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (visibleChannels.isEmpty()) {
                item {
                    TvEmptyState(selectedCategory.name, searchQuery)
                }
            }
            itemsIndexed(visibleChannels, key = { _, channel -> channel.stableFavoriteId() }) { index, channel ->
                val hasPreviousSelection = visibleChannels.any {
                    it.stableFavoriteId() == lastSelectedChannelId
                }
                val shouldRequestFocus = if (hasPreviousSelection) {
                    channel.stableFavoriteId() == lastSelectedChannelId
                } else {
                    index == 0
                }
                TvChannelRow(
                    channel = channel,
                    isFavorite = channel.stableFavoriteId() in favoriteChannelIds,
                    modifier = if (shouldRequestFocus) {
                        Modifier.focusRequester(firstChannelFocusRequester)
                    } else {
                        Modifier
                    },
                    onToggleFavorite = { onToggleFavorite(channel) },
                    onClick = { onChannelSelected(channel) },
                )
            }
        }
    }
}

@Composable
private fun TvCategoryChip(
    category: ChannelCategory,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var hasFocus by remember { mutableStateOf(false) }
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .onFocusChanged { hasFocus = it.hasFocus }
            .border(
                border = BorderStroke(
                    width = if (hasFocus) 3.dp else 0.dp,
                    color = if (hasFocus) Color.White else Color.Transparent,
                ),
                shape = RoundedCornerShape(8.dp),
            ),
        label = {
            Text(
                text = "${category.name} (${category.count})",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        },
    )
}

@Composable
private fun TvChannelRow(
    channel: Channel,
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
) {
    var hasFocus by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .onFocusChanged { hasFocus = it.hasFocus }
            .focusable()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (hasFocus) Color(0xFF26374A) else Color(0xFF172028),
        border = if (hasFocus) BorderStroke(3.dp, Color.White) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TvChannelLogo(
                logoUrl = channel.logoUrl,
                channelName = channel.name,
                modifier = Modifier.size(52.dp),
            )
            Text(
                text = channel.name,
                modifier = Modifier.weight(1f),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = onToggleFavorite) {
                Text(if (isFavorite) "★" else "☆")
            }
        }
    }
}

@Composable
private fun TvChannelLogo(
    logoUrl: String?,
    channelName: String,
    modifier: Modifier = Modifier,
) {
    val logo = produceState<ImageBitmap?>(initialValue = null, key1 = logoUrl) {
        value = logoUrl?.let { loadChannelLogo(it) }
    }

    Box(
        modifier = modifier.background(Color(0xFF2A333C), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (logo.value != null) {
            Image(
                bitmap = logo.value!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Text(
                text = channelName.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun TvEmptyState(
    selectedCategoryName: String,
    searchQuery: String,
) {
    val message = when {
        selectedCategoryName == FavoriteCategoryName && searchQuery.isBlank() ->
            "Todavia no tienes canales favoritos."
        searchQuery.isNotBlank() ->
            "No hay canales que coincidan con la busqueda."
        else ->
            "No hay canales en esta categoria."
    }

    Text(
        text = message,
        modifier = Modifier.padding(32.dp),
        color = Color.White,
        style = MaterialTheme.typography.titleMedium,
    )
}
