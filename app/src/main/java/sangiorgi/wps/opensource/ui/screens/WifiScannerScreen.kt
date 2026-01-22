package sangiorgi.wps.opensource.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sangiorgi.wps.opensource.R
import sangiorgi.wps.opensource.domain.models.*
import sangiorgi.wps.opensource.ui.scanner.WifiScannerViewModel
import sangiorgi.wps.opensource.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScannerScreen(viewModel: WifiScannerViewModel, onNetworkSelected: (WifiNetwork) -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val networks by viewModel.networks.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    var showFilters by remember { mutableStateOf(false) }
    var showOnlyWps by remember { mutableStateOf(true) }
    var sortBy by remember { mutableStateOf(SortOption.SIGNAL) }
    var dismissedWifiWarning by remember { mutableStateOf(false) }

    // Reset dismissed state when WiFi is enabled
    LaunchedEffect(uiState.isWifiEnabled) {
        if (uiState.isWifiEnabled) {
            dismissedWifiWarning = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.wifi_wps_scanner))
                        if (isScanning) {
                            Text(
                                stringResource(R.string.scanning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { !showFilters }) {
                        Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.filter))
                    }
                    IconButton(onClick = { viewModel.startScan() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.startScan() },
                icon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.scan)) },
                text = { Text(stringResource(R.string.scan_button)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // WiFi status and error messages
            if (!uiState.isWifiEnabled && !dismissedWifiWarning) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    Icons.Default.WifiOff,
                                    contentDescription = stringResource(R.string.wifi_off),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.wifi_is_disabled),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                            IconButton(onClick = { dismissedWifiWarning = true }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.dismiss),
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                        Text(
                            stringResource(R.string.enable_wifi_to_scan),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.toggleWifi() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                            ),
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(stringResource(R.string.enable_wifi))
                        }
                    }
                }
            }

            // Show other errors if any
            uiState.error?.let { error ->
                val wifiMustBeEnabledError = stringResource(R.string.wifi_must_be_enabled)
                if (error != wifiMustBeEnabledError || uiState.isWifiEnabled) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = { viewModel.clearError() },
                            ) {
                                Text(stringResource(R.string.dismiss))
                            }
                        }
                    }
                }
            }

            // Filter options
            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                FilterCard(
                    showOnlyWps = showOnlyWps,
                    onShowOnlyWpsChange = { showOnlyWps = it },
                    sortBy = sortBy,
                    onSortByChange = { sortBy = it },
                )
            }

            // Network count
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(R.string.networks_found, networks.size),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (showOnlyWps) {
                        Text(
                            stringResource(R.string.wps_count, networks.count { it.hasWps }),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Network list
            val filteredNetworks = remember(networks, showOnlyWps, sortBy) {
                networks
                    .filter { if (showOnlyWps) it.hasWps else true }
                    .sortedWith(
                        when (sortBy) {
                            SortOption.SIGNAL -> compareByDescending { it.signalLevel }
                            SortOption.NAME -> compareBy { it.ssid }
                            SortOption.CHANNEL -> compareBy { it.channel }
                            SortOption.SECURITY -> compareBy { it.security }
                        },
                    )
            }

            if (filteredNetworks.isEmpty()) {
                EmptyState(isScanning = isScanning)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(
                        items = filteredNetworks,
                        key = { it.bssid },
                    ) { network ->
                        NetworkCard(
                            network = network,
                            onClick = { onNetworkSelected(network) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterCard(
    showOnlyWps: Boolean,
    onShowOnlyWpsChange: (Boolean) -> Unit,
    sortBy: SortOption,
    onSortByChange: (SortOption) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.show_only_wps_networks), style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = showOnlyWps,
                    onCheckedChange = onShowOnlyWpsChange,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(stringResource(R.string.sort_by), style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SortOption.entries.forEach { option ->
                    FilterChip(
                        selected = sortBy == option,
                        onClick = { onSortByChange(option) },
                        label = { Text(stringResource(option.labelResId)) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetworkCard(network: WifiNetwork, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (network.hasWps) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left side - Network info
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = network.ssid,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (network.hasWps) {
                        WpsBadge(network.wpsInfo)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = network.bssid,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.channel_short, network.channel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = network.vendor,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SecurityChip(network.security)
                    Text(
                        text = stringResource(R.string.distance_format, network.distance),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (network.band != WifiBand.UNKNOWN) {
                        BandChip(network.band)
                    }
                }
            }

            // Right side - Signal indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SignalIndicator(
                    signalLevel = network.signalLevel,
                    signalStrength = network.signalStrength,
                )
                Text(
                    text = stringResource(R.string.signal_dbm_format, network.signalLevel),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WpsBadge(wpsInfo: WpsInfo?) {
    if (wpsInfo == null) return

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = when {
            wpsInfo.isLocked -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        },
    ) {
        Text(
            text = if (wpsInfo.isLocked) stringResource(R.string.wps_locked) else stringResource(R.string.wps),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun SecurityChip(security: SecurityType) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when (security) {
            SecurityType.OPEN -> SecurityOpen.copy(alpha = 0.2f)
            SecurityType.WEP -> SecurityWep.copy(alpha = 0.2f)
            SecurityType.WPA -> SecurityWpa.copy(alpha = 0.2f)
            SecurityType.WPA2 -> SecurityWpa2.copy(alpha = 0.2f)
            SecurityType.WPA3 -> SecurityWpa3.copy(alpha = 0.2f)
            else -> MaterialTheme.colorScheme.secondaryContainer
        },
    ) {
        Text(
            text = security.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = when (security) {
                SecurityType.OPEN -> SecurityOpen
                SecurityType.WEP -> SecurityWep
                SecurityType.WPA -> SecurityWpa
                SecurityType.WPA2 -> SecurityWpa2
                SecurityType.WPA3 -> SecurityWpa3
                else -> MaterialTheme.colorScheme.onSecondaryContainer
            },
        )
    }
}

@Composable
private fun BandChip(band: WifiBand) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Text(
            text = when (band) {
                WifiBand.BAND_2_4_GHZ -> stringResource(R.string.band_2_4_ghz)
                WifiBand.BAND_5_GHZ -> stringResource(R.string.band_5_ghz)
                WifiBand.BAND_6_GHZ -> stringResource(R.string.band_6_ghz)
                else -> ""
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
private fun SignalIndicator(@Suppress("UNUSED_PARAMETER") signalLevel: Int, signalStrength: SignalStrength) {
    val color = when (signalStrength) {
        SignalStrength.EXCELLENT -> SignalExcellent
        SignalStrength.GOOD -> SignalGood
        SignalStrength.FAIR -> SignalFair
        SignalStrength.WEAK -> SignalWeak
    }

    Icon(
        imageVector = Icons.Default.Wifi,
        contentDescription = stringResource(R.string.signal_strength),
        tint = color,
        modifier = Modifier.size(32.dp),
    )
}

@Composable
private fun EmptyState(isScanning: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = if (isScanning) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isScanning) {
                    stringResource(
                        R.string.scanning_for_networks,
                    )
                } else {
                    stringResource(R.string.no_networks_found)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

enum class SortOption(val labelResId: Int) {
    SIGNAL(R.string.sort_signal),
    NAME(R.string.sort_name),
    CHANNEL(R.string.sort_channel),
    SECURITY(R.string.sort_security),
}
