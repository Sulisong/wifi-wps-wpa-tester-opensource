package sangiorgi.wps.opensource.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sangiorgi.wps.opensource.R
import sangiorgi.wps.opensource.algorithm.PinGeneratorService
import javax.inject.Inject

@HiltViewModel
class PinSelectionViewModel @Inject constructor(
    private val pinGeneratorService: PinGeneratorService,
) : ViewModel() {

    private val _pins = MutableStateFlow<List<PinOption>>(emptyList())
    val pins: StateFlow<List<PinOption>> = _pins.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadPins(bssid: String?, ssid: String?, defaultPinLabel: String) {
        if (bssid.isNullOrEmpty()) {
            _pins.value = listOf(PinOption("12345670", defaultPinLabel, false))
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val generatedPins = pinGeneratorService.generateAllPins(bssid, ssid)
                _pins.value = generatedPins.map { pinWithSource ->
                    PinOption(
                        pin = pinWithSource.pin,
                        description = pinWithSource.source,
                        isFromDatabase = pinWithSource.isFromDatabase,
                    )
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}

@Composable
fun PinSelectionDialog(
    bssid: String? = null,
    ssid: String? = null,
    onDismiss: () -> Unit,
    onPinSelected: (List<String>) -> Unit,
    viewModel: PinSelectionViewModel = hiltViewModel(),
) {
    val defaultWpsPinLabel = stringResource(R.string.default_wps_pin)
    val pins by remember { viewModel.pins }.let { viewModel.pins }
        .let { mutableStateOf(it.value) }
        .also { state ->
            LaunchedEffect(bssid, ssid) {
                viewModel.loadPins(bssid, ssid, defaultWpsPinLabel)
                viewModel.pins.collect { state.value = it }
            }
        }

    val isLoading by remember { viewModel.isLoading }.let { viewModel.isLoading }
        .let { mutableStateOf(it.value) }
        .also { state ->
            LaunchedEffect(Unit) {
                viewModel.isLoading.collect { state.value = it }
            }
        }

    var selectedPins by remember(pins) { mutableStateOf(pins.map { it.pin }) }
    var customPin by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }

    // Update selected pins when pins list changes
    LaunchedEffect(pins) {
        selectedPins = pins.map { it.pin }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.select_pins_to_test),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = !showCustomInput,
                        onClick = {
                            showCustomInput = false
                            customPin = ""
                        },
                        label = { Text(stringResource(R.string.algorithm_pins)) },
                    )
                    FilterChip(
                        selected = showCustomInput,
                        onClick = { showCustomInput = true },
                        label = { Text(stringResource(R.string.custom_pin)) },
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (showCustomInput) {
                    // Custom PIN Input
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                        ) {
                            OutlinedTextField(
                                value = customPin,
                                onValueChange = { value ->
                                    if (value.length <= 8 && value.all { it.isDigit() }) {
                                        customPin = value
                                    }
                                },
                                label = { Text(stringResource(R.string.custom_pin)) },
                                placeholder = { Text(stringResource(R.string.enter_8_digit_pin)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                supportingText = {
                                    Text(stringResource(R.string.digits_count, customPin.length))
                                },
                                isError = customPin.isNotEmpty() && customPin.length != 8,
                            )

                            // Calculate checksum button
                            if (customPin.length == 7) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        customPin = calculateWpsChecksum(customPin)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Default.Calculate, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.calculate_checksum))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    // PIN Selection List
                    if (isLoading) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.loading_pins))
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.selected_pins_count, selectedPins.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Select All action
                        FilterChip(
                            selected = selectedPins.size == pins.size,
                            onClick = {
                                selectedPins = if (selectedPins.size == pins.size) {
                                    emptyList()
                                } else {
                                    pins.map { it.pin }
                                }
                            },
                            label = { Text(stringResource(R.string.select_all)) },
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // PIN List
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(pins) { pinOption ->
                                PinSelectionItem(
                                    pinOption = pinOption,
                                    isSelected = pinOption.pin in selectedPins,
                                    onToggle = {
                                        selectedPins = if (pinOption.pin in selectedPins) {
                                            selectedPins - pinOption.pin
                                        } else {
                                            selectedPins + pinOption.pin
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalPins = if (showCustomInput) {
                                if (customPin.length == 8) {
                                    listOf(customPin)
                                } else {
                                    emptyList()
                                }
                            } else {
                                selectedPins
                            }
                            if (finalPins.isNotEmpty()) {
                                onPinSelected(finalPins)
                            }
                        },
                        enabled = if (showCustomInput) {
                            customPin.length == 8
                        } else {
                            selectedPins.isNotEmpty() && !isLoading
                        },
                    ) {
                        Text(stringResource(R.string.start_testing))
                    }
                }
            }
        }
    }
}

@Composable
private fun PinSelectionItem(pinOption: PinOption, isSelected: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pinOption.pin,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (pinOption.isFromDatabase) {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = null,
                            modifier = Modifier.height(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = pinOption.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (pinOption.isFromDatabase) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

data class PinOption(
    val pin: String,
    val description: String,
    val isFromDatabase: Boolean = false,
)

/**
 * Calculate WPS PIN checksum
 */
fun calculateWpsChecksum(pin: String): String {
    if (pin.length != 7) return pin

    val pinInt = pin.toIntOrNull() ?: return pin
    var accum = 0

    val tempPin = pinInt * 10
    accum += 3 * ((tempPin / 10000000) % 10)
    accum += 1 * ((tempPin / 1000000) % 10)
    accum += 3 * ((tempPin / 100000) % 10)
    accum += 1 * ((tempPin / 10000) % 10)
    accum += 3 * ((tempPin / 1000) % 10)
    accum += 1 * ((tempPin / 100) % 10)
    accum += 3 * ((tempPin / 10) % 10)

    val checksum = (10 - (accum % 10)) % 10
    return pin + checksum
}
