package sangiorgi.wps.opensource.utils

import android.content.Context
import android.util.Log
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sangiorgi.wps.opensource.domain.models.WpsInfo
import sangiorgi.wps.opensource.domain.models.WpsMethod
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scanner that uses the `iw` binary to get detailed WPS information
 * from wireless networks. Requires root access.
 */
@Singleton
class IwScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val rootChecker: RootChecker,
) {
    companion object {
        private const val TAG = "IwScanner"
        private const val IW_BINARY = "iw"
    }

    private val filesDir: File = context.filesDir
    private val iwPath: String = File(filesDir, IW_BINARY).absolutePath

    /**
     * Check if the iw binary is available and executable (quick synchronous check).
     * Does NOT check root - use isFullyAvailable() for that.
     */
    fun isBinaryAvailable(): Boolean {
        val iwFile = File(iwPath)
        val fileExists = iwFile.exists() && iwFile.canExecute()
        if (!fileExists) {
            Log.d(TAG, "iw binary not found or not executable at $iwPath")
            return false
        }
        return true
    }

    /**
     * Quick check using cached root status. Returns false if root hasn't been checked yet.
     * Use isFullyAvailable() for a definitive check that includes root request.
     */
    fun isAvailable(): Boolean {
        if (!isBinaryAvailable()) return false

        val cachedRoot = rootChecker.getCachedRootStatus()
        if (cachedRoot == null || !cachedRoot) {
            Log.d(TAG, "Root not available (cached: $cachedRoot), iw scanner disabled")
            return false
        }
        return true
    }

    /**
     * Check if the iw binary is available, executable, AND root is available.
     * This is a suspend function that may trigger a root request.
     */
    suspend fun isFullyAvailable(): Boolean {
        if (!isBinaryAvailable()) return false

        val hasRoot = rootChecker.isRootAvailable()
        if (!hasRoot) {
            Log.d(TAG, "Root not available, iw scanner disabled")
            return false
        }
        return true
    }

    /**
     * Get the wireless interface name (usually wlan0)
     */
    suspend fun getWirelessInterface(): String? = withContext(Dispatchers.IO) {
        try {
            val result = Shell.cmd("ls /sys/class/net/ | grep -E '^wlan'").exec()
            if (result.isSuccess && result.out.isNotEmpty()) {
                result.out.firstOrNull()?.trim()
            } else {
                // Fallback to wlan0
                "wlan0"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get wireless interface", e)
            "wlan0"
        }
    }

    /**
     * Get WPS information for all visible networks
     */
    suspend fun getAllWpsInfo(): Map<String, WpsInfo> = withContext(Dispatchers.IO) {
        if (!isFullyAvailable()) {
            Log.w(TAG, "iw binary not available or no root")
            return@withContext emptyMap()
        }

        val iface = getWirelessInterface() ?: return@withContext emptyMap()
        val libPath = filesDir.absolutePath

        try {
            // Run iw scan dump with proper library path (same pattern as wpa_supplicant)
            val cmd = "cd $libPath && export LD_LIBRARY_PATH=$libPath && ./$IW_BINARY dev $iface scan dump"
            Log.d(TAG, "Running iw command: $cmd")
            val result = Shell.cmd(cmd).exec()

            if (!result.isSuccess) {
                Log.e(TAG, "iw scan dump failed: ${result.err.joinToString("\n")}")
                return@withContext emptyMap()
            }

            Log.d(TAG, "iw scan dump returned ${result.out.size} lines")
            if (result.out.isEmpty()) {
                Log.w(TAG, "iw scan dump returned empty output")
            } else {
                // Log first few lines to debug
                result.out.take(10).forEach { Log.d(TAG, "iw output: $it") }
            }

            parseAllWpsInfoFromIwOutput(result.out)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all WPS info", e)
            emptyMap()
        }
    }

    /**
     * Parse WPS information for all BSSIDs from iw scan dump output
     */
    private fun parseAllWpsInfoFromIwOutput(lines: List<String>): Map<String, WpsInfo> {
        Log.d(TAG, "Parsing iw output: ${lines.size} lines")

        val result = mutableMapOf<String, WpsInfo>()
        var currentBssid: String? = null
        var wpsState: Int? = null
        var configMethods: Int? = null
        var deviceName: String? = null
        var manufacturer: String? = null
        var modelName: String? = null
        var modelNumber: String? = null

        for (line in lines) {
            val trimmed = line.trim()

            // Check for new BSS entry
            if (trimmed.startsWith("BSS ")) {
                // Save previous BSS
                currentBssid?.let { bssid ->
                    val wpsInfo = buildWpsInfo(
                        wpsState,
                        configMethods,
                        deviceName,
                        manufacturer,
                        modelName,
                        modelNumber,
                    )
                    if (wpsInfo != null) {
                        Log.d(
                            TAG,
                            "Found WPS for $bssid: PBC=${wpsInfo.isPbcSupported}, " +
                                "PIN=${wpsInfo.isPinSupported}, configMethods=0x${configMethods?.toString(16)}",
                        )
                        result[bssid.uppercase()] = wpsInfo
                    }
                }

                // Extract BSSID
                currentBssid = extractBssid(trimmed)
                wpsState = null
                configMethods = null
                deviceName = null
                manufacturer = null
                modelName = null
                modelNumber = null
                continue
            }

            // Parse WPS information
            when {
                trimmed.startsWith("* Wi-Fi Protected Setup State:") -> {
                    wpsState = extractNumber(trimmed)
                    Log.d(TAG, "  WPS State for $currentBssid: $wpsState")
                }
                trimmed.startsWith("* Config methods:") -> {
                    configMethods = extractConfigMethods(trimmed)
                    Log.d(TAG, "  Config methods for $currentBssid: 0x${configMethods.toString(16)}")
                }
                trimmed.startsWith("* Device name:") -> {
                    deviceName = extractValue(trimmed)
                }
                trimmed.startsWith("* Manufacturer:") -> {
                    manufacturer = extractValue(trimmed)
                }
                trimmed.startsWith("* Model:") -> {
                    modelName = extractValue(trimmed)
                }
                trimmed.startsWith("* Model Number:") -> {
                    modelNumber = extractValue(trimmed)
                }
            }
        }

        // Save last BSS
        currentBssid?.let { bssid ->
            val wpsInfo = buildWpsInfo(
                wpsState,
                configMethods,
                deviceName,
                manufacturer,
                modelName,
                modelNumber,
            )
            if (wpsInfo != null) {
                Log.d(
                    TAG,
                    "Found WPS for $bssid: PBC=${wpsInfo.isPbcSupported}, PIN=${wpsInfo.isPinSupported}",
                )
                result[bssid.uppercase()] = wpsInfo
            }
        }

        Log.d(TAG, "Parsed ${result.size} networks with WPS info")
        return result
    }

    private fun extractBssid(line: String): String? {
        // Line format: "BSS aa:bb:cc:dd:ee:ff(on wlan0)" or "BSS aa:bb:cc:dd:ee:ff"
        val regex = Regex("BSS ([0-9a-fA-F:]{17})")
        return regex.find(line)?.groupValues?.get(1)
    }

    private fun extractNumber(line: String): Int? {
        val regex = Regex("(\\d+)")
        return regex.find(line)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractValue(line: String): String? {
        val parts = line.split(":", limit = 2)
        return if (parts.size == 2) parts[1].trim() else null
    }

    private fun extractConfigMethods(line: String): Int {
        // Config methods is a hex value like 0x008c
        val regex = Regex("0x([0-9a-fA-F]+)")
        val match = regex.find(line)
        return match?.groupValues?.get(1)?.toIntOrNull(16) ?: 0
    }

    private fun buildWpsInfo(
        wpsState: Int?,
        configMethods: Int?,
        deviceName: String?,
        manufacturer: String?,
        modelName: String?,
        modelNumber: String?,
    ): WpsInfo? {
        // If no WPS state found, WPS is not available
        if (wpsState == null) return null

        val methods = mutableListOf<WpsMethod>()
        val config = configMethods ?: 0

        // WPS Config Methods bit flags:
        // 0x0001 = USB
        // 0x0002 = Ethernet
        // 0x0004 = Label
        // 0x0008 = Display
        // 0x0010 = External NFC Token
        // 0x0020 = Integrated NFC Token
        // 0x0040 = NFC Interface
        // 0x0080 = Push Button
        // 0x0100 = Keypad
        // 0x0280 = Virtual Push Button
        // 0x0480 = Physical Push Button
        // 0x2008 = Virtual Display PIN

        val hasPushButton = (config and 0x0080) != 0 ||
            (config and 0x0280) != 0 ||
            (config and 0x0480) != 0
        // Label (0x0004), Display (0x0008), Keypad (0x0100), Virtual Display PIN (0x2008)
        val hasPin = (config and 0x0004) != 0 ||
            (config and 0x0008) != 0 ||
            (config and 0x0100) != 0 ||
            (config and 0x2008) != 0
        val hasNfc = (config and 0x0010) != 0 ||
            (config and 0x0020) != 0 ||
            (config and 0x0040) != 0

        if (hasPushButton) methods.add(WpsMethod.PUSH_BUTTON)
        if (hasPin) methods.add(WpsMethod.PIN)
        if (hasNfc) methods.add(WpsMethod.NFC)

        // WPS State:
        // 1 = Not Configured
        // 2 = Configured
        wpsState == 2

        // Note: iw doesn't directly report locked state, so isLocked is always false
        return WpsInfo(
            isEnabled = true,
            isPbcSupported = hasPushButton,
            isPinSupported = hasPin,
            isLocked = false,
            configMethods = methods,
            deviceName = deviceName,
            manufacturer = manufacturer,
            modelName = modelName,
            modelNumber = modelNumber,
            isFromIw = true,
        )
    }
}
