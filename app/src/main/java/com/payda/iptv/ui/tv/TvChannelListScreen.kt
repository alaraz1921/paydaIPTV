package com.payda.iptv.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.payda.iptv.data.Channel
import com.payda.iptv.data.stableFavoriteId
import com.payda.iptv.epg.EpgData
import com.payda.iptv.epg.timeRangeText
import com.payda.iptv.ui.playlist.AllCategoryName
import com.payda.iptv.ui.playlist.ChannelCategory
import com.payda.iptv.ui.playlist.FavoriteCategoryName
import com.payda.iptv.ui.playlist.buildChannelCategories
import com.payda.iptv.ui.playlist.filterChannels
import com.payda.iptv.ui.playlist.loadChannelLogo
import com.payda.iptv.ui.theme.PayDaTextField
import com.payda.iptv.ui.theme.PayDaTextFieldColors
import java.time.Instant

@Composable
fun TvChannelListScreen(
    channels: List<Channel>,
    selectedCategoryName: String,
    categorySearchQuery: String,
    favoriteChannelIds: Set<String>,
    epgData: EpgData?,
    epgNow: Instant,
    epgMessage: String?,
    lastSelectedChannelId: String?,
    onCategorySelected: (String) -> Unit,
    onCategorySearchChange: (String) -> Unit,
    onClearCategorySearch: () -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onChannelSelected: (Channel, List<Channel>) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackToHome)

    val categories = remember(channels, favoriteChannelIds) {
        buildChannelCategories(channels, favoriteChannelIds)
    }
    val categorySearch = categorySearchQuery.trim()
    val visibleCategories = remember(categories, categorySearch) {
        if (categorySearch.isBlank()) {
            categories
        } else {
            categories.filter { it.name.contains(categorySearch, ignoreCase = true) }
        }.ifEmpty { categories.take(1) }
    }
    val selectedCategory = remember(categories, selectedCategoryName) {
        categories.firstOrNull { it.name == selectedCategoryName } ?: categories.first()
    }
    val visibleChannels = remember(channels, selectedCategory, favoriteChannelIds) {
        filterChannels(
            channels = channels,
            selectedCategoryName = selectedCategory.name,
            searchQuery = "",
            favoriteChannelIds = favoriteChannelIds,
        )
    }
    val selectedCategoryFocusRequester = remember { FocusRequester() }
    val firstChannelFocusRequester = remember { FocusRequester() }
    val channelFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    LaunchedEffect(Unit) {
        selectedCategoryFocusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackground)
            .padding(horizontal = 42.dp, vertical = 30.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "PayDa IPTV",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            PayDaTextField(
                value = categorySearchQuery,
                onValueChange = onCategorySearchChange,
                modifier = Modifier.weight(1f),
                label = { Text("Buscar categoria") },
                singleLine = true,
                isTvStyle = true,
                colors = PayDaTextFieldColors(),
                trailingIcon = {
                    if (categorySearchQuery.isNotBlank()) {
                        TextButton(onClick = onClearCategorySearch) {
                            Text("Limpiar")
                        }
                    }
                },
            )
            Text(
                text = selectedCategory.name,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (!epgMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = epgMessage,
                color = Color(0xFFFCA5A5),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            LazyColumn(
                modifier = Modifier.width(285.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visibleCategories, key = { it.name }) { category ->
                    TvCategoryRow(
                        category = category,
                        selected = category.name == selectedCategory.name,
                        modifier = if (category.name == selectedCategory.name) {
                            Modifier.focusRequester(selectedCategoryFocusRequester)
                        } else {
                            Modifier
                        },
                        onMoveRight = {
                            if (visibleChannels.isNotEmpty()) {
                                val targetId = lastSelectedChannelId?.takeIf { id ->
                                    visibleChannels.any { it.stableFavoriteId() == id }
                                } ?: visibleChannels.first().stableFavoriteId()
                                channelFocusRequesters[targetId]?.requestFocus()
                                    ?: firstChannelFocusRequester.requestFocus()
                            }
                        },
                        onClick = { onCategorySelected(category.name) },
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (visibleChannels.isEmpty()) {
                    item {
                        TvEmptyState(selectedCategory.name)
                    }
                }
                itemsIndexed(
                    visibleChannels,
                    key = { _, channel -> channel.stableFavoriteId() },
                ) { index, channel ->
                    val channelId = channel.stableFavoriteId()
                    val rememberedFocusRequester = remember(channelId) { FocusRequester() }
                    val shouldRequestInitialFocus = if (lastSelectedChannelId != null) {
                        channelId == lastSelectedChannelId
                    } else {
                        index == 0
                    }
                    val attachedFocusRequester = if (shouldRequestInitialFocus) {
                        firstChannelFocusRequester
                    } else {
                        rememberedFocusRequester
                    }
                    channelFocusRequesters[channelId] = attachedFocusRequester
                    TvChannelCard(
                        channel = channel,
                        isFavorite = channelId in favoriteChannelIds,
                        epgData = epgData,
                        epgNow = epgNow,
                        modifier = Modifier
                            .focusRequester(
                                attachedFocusRequester,
                            )
                            .onPreviewKeyEvent { event ->
                                if (
                                    event.type == KeyEventType.KeyDown &&
                                    event.key == Key.DirectionLeft &&
                                    index % 3 == 0
                                ) {
                                    selectedCategoryFocusRequester.requestFocus()
                                    true
                                } else {
                                    false
                                }
                            },
                        onClick = { onChannelSelected(channel, visibleChannels) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TvCategoryRow(
    category: ChannelCategory,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onMoveRight: () -> Unit,
    onClick: () -> Unit,
) {
    var hasFocus by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .onFocusChanged { hasFocus = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                    onMoveRight()
                    true
                } else {
                    false
                }
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = when {
            hasFocus -> Color(0xFF26374A)
            selected -> Color(0xFF1F2933)
            else -> Color.Transparent
        },
        border = if (hasFocus) BorderStroke(2.dp, Color.White) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = category.name,
                modifier = Modifier.weight(1f),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = category.count.toString(),
                color = Color(0xFFCBD5E1),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
private fun TvChannelCard(
    channel: Channel,
    isFavorite: Boolean,
    epgData: EpgData?,
    epgNow: Instant,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var hasFocus by remember { mutableStateOf(false) }
    val epgInfo = epgData?.programmeFor(channel, epgNow)
    val currentProgramme = epgInfo?.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.55f)
            .onFocusChanged { hasFocus = it.hasFocus }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (hasFocus) Color(0xFF26374A) else Color(0xFF151C22),
        border = BorderStroke(
            width = if (hasFocus) 3.dp else 1.dp,
            color = if (hasFocus) Color.White else Color(0xFF2F3B46),
        ),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF10161B)),
                contentAlignment = Alignment.Center,
            ) {
                TvChannelLogo(
                    logoUrl = channel.logoUrl,
                    channelName = channel.name,
                    modifier = Modifier.size(88.dp),
                )
                Text(
                    text = if (isFavorite) "*" else "",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = channel.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (currentProgramme != null) {
                    Text(
                        text = "${currentProgramme.title} · ${currentProgramme.timeRangeText()}",
                        color = Color(0xFFCBD5E1),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
internal fun TvChannelLogo(
    logoUrl: String?,
    channelName: String,
    modifier: Modifier = Modifier,
) {
    val logo = produceState<ImageBitmap?>(initialValue = null, key1 = logoUrl) {
        value = logoUrl?.let { loadChannelLogo(it) }
    }

    Box(
        modifier = modifier.background(Color(0xFF26313A), RoundedCornerShape(8.dp)),
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
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TvEmptyState(
    selectedCategoryName: String,
) {
    val message = when (selectedCategoryName) {
        FavoriteCategoryName -> "Todavia no tienes canales favoritos."
        AllCategoryName -> "No hay canales disponibles."
        else -> "No hay canales en esta categoria."
    }

    Text(
        text = message,
        modifier = Modifier.padding(32.dp),
        color = Color.White,
        style = MaterialTheme.typography.titleMedium,
    )
}
