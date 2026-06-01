package com.kapcode.open.macropad.kmps.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdSize
import com.kapcode.open.macropad.kmps.AdmobBanner
import com.kapcode.open.macropad.kmps.models.MarketplaceItem

import com.kapcode.`open`.macropad.kmps.Res
import com.kapcode.`open`.macropad.kmps.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    items: List<MarketplaceItem>,
    isPro: Boolean,
    isDeveloperMode: Boolean,
    onProToggle: (Boolean) -> Unit,
    onDownload: (MarketplaceItem) -> Unit,
    modifier: Modifier = Modifier,
    adsDisabled: Boolean = false
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredItems = items.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.author.contains(searchQuery, ignoreCase = true) ||
        it.description.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (isDeveloperMode) {
            ProAccessSection(isPro = isPro, onProToggle = onProToggle)
        }
        
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = { Text(stringResource(Res.string.search_marketplace)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(Res.string.marketplace_coming_soon),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = stringResource(Res.string.marketplace_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredItems) { item ->
                        MarketplaceItemCard(item = item, onDownload = { onDownload(item) })
                    }
                }
            }
        }

        // Add a large banner at the bottom of the Marketplace if ads are enabled
        if (!adsDisabled) {
            AdmobBanner(
                location = AdLocation.MARKETPLACE,
                modifier = Modifier.padding(bottom = 8.dp),
                adSize = AdSize.LARGE_BANNER
            )
        }
    }
}

@Composable
fun ProAccessSection(
    isPro: Boolean,
    onProToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPro) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPro) stringResource(Res.string.pro_access_active) else stringResource(Res.string.buy_pro),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isPro) "12-hour global server access enabled." else "Unlock global server access for 12 hours.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Switch(
                checked = isPro,
                onCheckedChange = onProToggle
            )
        }
    }
}

@Composable
fun MarketplaceItemCard(
    item: MarketplaceItem,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "by ${item.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(onClick = onDownload) {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = "Download",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: @Composable () -> Unit,
    leadingIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
        )
    )
}
