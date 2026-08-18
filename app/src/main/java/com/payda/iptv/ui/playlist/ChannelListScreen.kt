package com.payda.iptv.ui.playlist

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.payda.iptv.data.Channel
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ChannelListScreen(
    channels: List<Channel>,
    selectedCategoryName: String,
    playlistUrl: String,
    onCategorySelected: (String) -> Unit,
    onChannelSelected: (Channel) -> Unit,
    onChangePlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories = remember(channels) { buildChannelCategories(channels) }
    val selectedCategory = remember(categories, selectedCategoryName) {
        categories.firstOrNull { it.name == selectedCategoryName } ?: categories.first()
    }
    val visibleChannels = remember(channels, selectedCategory) {
        if (selectedCategory.name == AllCategoryName) {
            channels
        } else {
            channels.filter { normalizedCategoryName(it.group) == selectedCategory.name }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Canales",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "${visibleChannels.size} de ${channels.size} canales",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = playlistUrl,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onChangePlaylist) {
            Text("Cambiar lista")
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(categories, key = { it.name }) { category ->
                FilterChip(
                    selected = category.name == selectedCategory.name,
                    onClick = { onCategorySelected(category.name) },
                    label = { Text("${category.name} (${category.count})") },
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(visibleChannels) { channel ->
                ChannelRow(
                    channel = channel,
                    onClick = { onChannelSelected(channel) },
                )
            }
        }
    }
}

@Composable
private fun ChannelRow(
    channel: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .focusable()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChannelLogo(
                logoUrl = channel.logoUrl,
                channelName = channel.name,
                modifier = Modifier.size(48.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!channel.group.isNullOrBlank()) {
                    Text(
                        text = channel.group,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelLogo(
    logoUrl: String?,
    channelName: String,
    modifier: Modifier = Modifier,
) {
    val logo = produceState<ImageBitmap?>(initialValue = null, key1 = logoUrl) {
        value = logoUrl?.let { loadImage(it) }
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
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
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private suspend fun loadImage(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        URL(url).openStream().use { input ->
            BitmapFactory.decodeStream(input)?.asImageBitmap()
        }
    }.getOrNull()
}

internal const val AllCategoryName = "Todos"
internal const val UncategorizedName = "Sin categoria"

internal data class ChannelCategory(
    val name: String,
    val count: Int,
)

internal fun buildChannelCategories(channels: List<Channel>): List<ChannelCategory> {
    val categoryCounts = linkedMapOf<String, Int>()
    channels.forEach { channel ->
        val categoryName = normalizedCategoryName(channel.group)
        categoryCounts[categoryName] = categoryCounts.getOrDefault(categoryName, 0) + 1
    }

    return listOf(ChannelCategory(AllCategoryName, channels.size)) +
        categoryCounts.map { (name, count) -> ChannelCategory(name, count) }
}

internal fun normalizedCategoryName(group: String?): String {
    return group?.trim()?.takeIf { it.isNotEmpty() } ?: UncategorizedName
}
