package sangiorgi.wps.opensource.utils

import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class to check and manage root access using libsu.
 * All operations are suspend functions to ensure they run on IO dispatcher.
 */
@Singleton
class RootChecker @Inject constructor() {

    companion object {
        private const val TAG = "RootChecker"
    }

    // Cache the root status after first request
    private var rootRequested = false
    private var rootGranted = false

    // Mutex to prevent concurrent root requests
    private val rootCheckMutex = Mutex()

    /**
     * Check if the device has root access available.
     * On first call, this will trigger the root permission request from Magisk/SuperSU.
     *
     * This is a suspend function that performs I/O on the appropriate dispatcher.
     */
    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        rootCheckMutex.withLock {
            // If we already requested and got a result, return cached value
            if (rootRequested) {
                return@withContext rootGranted
            }

            // First check if already granted (quick check)
            val alreadyGranted = Shell.isAppGrantedRoot()
            if (alreadyGranted == true) {
                rootRequested = true
                rootGranted = true
                Log.d(TAG, "Root already granted")
                return@withContext true
            }

            // If not already granted, we need to request it by getting a shell
            // This triggers the Magisk/SuperSU permission dialog
            return@withContext requestRootAccessInternal()
        }
    }

    /**
     * Request root access. This will trigger the Magisk/SuperSU permission dialog.
     * Call this early in the app lifecycle to prompt the user.
     *
     * This is a suspend function that performs I/O on the appropriate dispatcher.
     */
    suspend fun requestRootAccess(): Boolean = withContext(Dispatchers.IO) {
        rootCheckMutex.withLock {
            if (rootRequested && rootGranted) {
                return@withContext true
            }
            return@withContext requestRootAccessInternal()
        }
    }

    /**
     * Internal root access request - must be called within the mutex lock.
     */
    private fun requestRootAccessInternal(): Boolean {
        Log.d(TAG, "Requesting root access...")

        return try {
            // Get a root shell - this triggers the permission request
            val shell = Shell.getShell()
            rootRequested = true
            rootGranted = shell.isRoot

            if (rootGranted) {
                Log.d(TAG, "Root access granted")
            } else {
                Log.d(TAG, "Root access denied or not available")
            }

            rootGranted
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting root access", e)
            rootRequested = true
            rootGranted = false
            false
        }
    }

    /**
     * Get the cached root status without triggering a new request.
     * Returns null if root has not been checked yet.
     */
    fun getCachedRootStatus(): Boolean? {
        return if (rootRequested) rootGranted else null
    }
}
