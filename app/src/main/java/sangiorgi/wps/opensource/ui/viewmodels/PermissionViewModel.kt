package sangiorgi.wps.opensource.ui.viewmodels

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import sangiorgi.wps.opensource.permissions.PermissionManager
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val permissionManager = PermissionManager(context)

    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    init {
        checkPermissions()
    }

    fun checkPermissions() {
        val hasPermissions = permissionManager.hasWifiScanningPermissions()
        val locationEnabled = permissionManager.isLocationEnabled()

        _permissionState.update {
            it.copy(
                hasPermissions = hasPermissions,
                isLocationEnabled = locationEnabled,
                isReady = hasPermissions && locationEnabled,
            )
        }
    }

    fun onPermissionsResult(permissions: Map<String, Boolean>, activity: Activity?) {
        val allGranted = permissions.entries.all { it.value }

        if (allGranted) {
            checkPermissions()
            if (_permissionState.value.hasPermissions) {
                _permissionState.update {
                    it.copy(
                        event = PermissionEvent.PermissionsGranted,
                    )
                }
            } else {
                // Check if location is enabled
                if (!permissionManager.isLocationEnabled()) {
                    _permissionState.update {
                        it.copy(
                            event = PermissionEvent.LocationDisabled,
                        )
                    }
                }
            }
        } else {
            val deniedPermissions = permissions.entries
                .filter { !it.value }
                .map { it.key }

            // Check if we should show rationale
            val shouldShowRationale = activity?.let { act ->
                deniedPermissions.any { permission ->
                    permissionManager.shouldShowRationale(act, permission)
                }
            } ?: false

            if (!shouldShowRationale && _permissionState.value.hasRequestedPermissions) {
                // User selected "Don't ask again"
                _permissionState.update {
                    it.copy(
                        event = PermissionEvent.PermanentlyDenied,
                    )
                }
            } else {
                _permissionState.update {
                    it.copy(
                        event = PermissionEvent.SomeDenied(deniedPermissions),
                    )
                }
            }
        }

        _permissionState.update {
            it.copy(hasRequestedPermissions = true)
        }
    }

    fun consumeEvent() {
        _permissionState.update { it.copy(event = null) }
    }

    fun onPermissionsGrantedManually() {
        checkPermissions()
        _permissionState.update {
            it.copy(isReady = it.hasPermissions && it.isLocationEnabled)
        }
    }

    data class PermissionState(
        val hasPermissions: Boolean = false,
        val isLocationEnabled: Boolean = false,
        val hasRequestedPermissions: Boolean = false,
        val isReady: Boolean = false,
        val event: PermissionEvent? = null,
    )

    sealed class PermissionEvent {
        data object PermissionsGranted : PermissionEvent()
        data object LocationDisabled : PermissionEvent()
        data object PermanentlyDenied : PermissionEvent()
        data class SomeDenied(val permissions: List<String>) : PermissionEvent()
    }
}
