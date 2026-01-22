package sangiorgi.wps.opensource

import android.app.Application
import android.util.Log
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sangiorgi.wps.opensource.data.assets.WpaToolsInitializer

@HiltAndroidApp
class WpsApplication : Application() {

    companion object {
        private const val TAG = "WpsApplication"

        private val _initializationState = MutableStateFlow<InitializationState>(InitializationState.Loading)
        val initializationState: StateFlow<InitializationState> = _initializationState.asStateFlow()
    }

    sealed class InitializationState {
        data object Loading : InitializationState()
        data object Ready : InitializationState()
        data class Failed(val error: String) : InitializationState()
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Shell configuration once for the entire app (this is fast)
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(15),
        )

        // Initialize WPA tools and vendor database asynchronously to avoid ANR
        Log.d(TAG, "Starting async initialization of WPA tools...")
        WpaToolsInitializer.initializeAsync(this)
            .thenAccept { success ->
                if (success) {
                    Log.d(TAG, "WPA tools and vendor database initialized successfully")
                    _initializationState.value = InitializationState.Ready
                } else {
                    Log.e(TAG, "Failed to initialize required assets")
                    _initializationState.value = InitializationState.Failed("Failed to initialize WPA tools")
                }
            }
            .exceptionally { throwable ->
                Log.e(TAG, "Exception during initialization", throwable)
                _initializationState.value = InitializationState.Failed(
                    throwable.message ?: "Unknown initialization error",
                )
                null
            }
    }
}
