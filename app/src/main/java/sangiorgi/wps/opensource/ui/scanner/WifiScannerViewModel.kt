package sangiorgi.wps.opensource.ui.scanner

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sangiorgi.wps.opensource.R
import sangiorgi.wps.opensource.domain.models.WifiNetwork
import javax.inject.Inject

@HiltViewModel
class WifiScannerViewModel @Inject constructor(
    private val application: Application,
    private val wifiScannerManager: WifiScannerManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WifiScannerUiState())
    val uiState: StateFlow<WifiScannerUiState> = _uiState.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    val networks: StateFlow<List<WifiNetwork>> = wifiScannerManager.scanResults
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun startScan() {
        viewModelScope.launch {
            // First check if WiFi is enabled
            if (!wifiScannerManager.isWifiEnabled()) {
                _uiState.update { currentState ->
                    currentState.copy(
                        error = application.getString(R.string.wifi_must_be_enabled),
                        isWifiEnabled = false,
                    )
                }
                return@launch
            }

            _isScanning.value = true
            val success = wifiScannerManager.startScan()

            if (!success) {
                // If WiFi is enabled but scan failed, it's likely a permission issue
                _uiState.update { currentState ->
                    currentState.copy(
                        error = application.getString(R.string.error_location_permission_required),
                    )
                }
            } else {
                // Clear any previous errors on successful scan
                _uiState.update { currentState ->
                    currentState.copy(error = null)
                }
            }

            // Keep scanning indicator for 2 seconds minimum
            delay(2000)
            _isScanning.value = false
        }
    }

    fun toggleWifi() {
        viewModelScope.launch {
            val isEnabled = wifiScannerManager.isWifiEnabled()
            val success = wifiScannerManager.setWifiEnabled(!isEnabled)

            if (!success) {
                _uiState.update { currentState ->
                    currentState.copy(
                        error = application.getString(R.string.error_cannot_toggle_wifi),
                    )
                }
            } else {
                // Update WiFi status after toggling
                delay(1000) // Give system time to change WiFi state
                updateWifiStatus()

                // Start scanning if WiFi was just enabled
                if (!isEnabled && wifiScannerManager.isWifiEnabled()) {
                    startScan()
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun updateWifiStatus() {
        _uiState.update { currentState ->
            currentState.copy(
                isWifiEnabled = wifiScannerManager.isWifiEnabled(),
            )
        }
    }
}

data class WifiScannerUiState(
    val error: String? = null,
    val isWifiEnabled: Boolean = true,
)
