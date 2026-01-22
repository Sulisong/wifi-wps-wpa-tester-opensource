package sangiorgi.wps.opensource.domain.models

import android.net.wifi.ScanResult
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Locale
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

/**
 * Enhanced WiFi network model with comprehensive WPS attributes
 */
@Parcelize
data class WifiNetwork(
    val bssid: String,
    val ssid: String,
    val signalLevel: Int,
    val frequency: Int,
    val capabilities: String,
    val vendor: String = "Unknown",
    val timestamp: Long = System.currentTimeMillis(),
    val isHidden: Boolean = false,
    val wpsInfo: WpsInfo? = null,
) : Parcelable {

    val channel: Int
        get() = frequencyToChannel(frequency)

    val band: WifiBand
        get() = when {
            frequency in 2412..2484 -> WifiBand.BAND_2_4_GHZ
            frequency in 5170..5825 -> WifiBand.BAND_5_GHZ
            frequency in 5925..7125 -> WifiBand.BAND_6_GHZ
            else -> WifiBand.UNKNOWN
        }

    val distance: Double
        get() = calculateDistance(frequency, signalLevel)

    val security: SecurityType
        get() = parseSecurityType(capabilities)

    val hasWps: Boolean
        get() = capabilities.contains("WPS", ignoreCase = true)

    val isWpsLocked: Boolean
        get() = wpsInfo?.isLocked ?: false

    val signalStrength: SignalStrength
        get() = when {
            signalLevel >= -50 -> SignalStrength.EXCELLENT
            signalLevel >= -60 -> SignalStrength.GOOD
            signalLevel >= -70 -> SignalStrength.FAIR
            else -> SignalStrength.WEAK
        }

    companion object {
        private const val DISTANCE_MHZ_M = 27.55

        /**
         * Create a WifiNetwork from a ScanResult
         * @param scanResult The Android ScanResult
         * @param vendor The vendor name from the MAC database
         * @param detailedWpsInfo Optional detailed WPS info from iw scanner (root required)
         */
        fun fromScanResult(
            scanResult: ScanResult,
            vendor: String? = null,
            detailedWpsInfo: WpsInfo? = null,
        ): WifiNetwork {
            // Use detailed WPS info from iw if available, otherwise fall back to capabilities parsing
            val wpsInfo = detailedWpsInfo ?: if (scanResult.capabilities.contains("WPS")) {
                WpsInfo.fromCapabilities(scanResult.capabilities)
            } else {
                null
            }

            // Use WifiSsid for Android 33+ or fallback to SSID for older versions
            val ssidString = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                scanResult.wifiSsid?.toString() ?: "*Hidden Network*"
            } else {
                @Suppress("DEPRECATION")
                scanResult.SSID.ifEmpty { "*Hidden Network*" }
            }

            return WifiNetwork(
                bssid = scanResult.BSSID,
                ssid = ssidString,
                signalLevel = scanResult.level,
                frequency = scanResult.frequency,
                capabilities = scanResult.capabilities,
                vendor = vendor ?: "Unknown",
                timestamp = scanResult.timestamp,
                isHidden = ssidString == "*Hidden Network*" || ssidString.isEmpty(),
                wpsInfo = wpsInfo,
            )
        }

        fun frequencyToChannel(freq: Int): Int {
            return when {
                freq in 2412..2484 -> (freq - 2412) / 5 + 1
                freq in 5170..5825 -> (freq - 5170) / 5 + 34
                freq in 5925..7125 -> (freq - 5925) / 5 + 1
                else -> -1
            }
        }

        fun calculateDistance(frequency: Int, level: Int): Double {
            return 10.0.pow((DISTANCE_MHZ_M - (20 * log10(frequency.toDouble())) + abs(level)) / 20.0)
        }

        private fun parseSecurityType(capabilities: String): SecurityType {
            return when {
                capabilities.contains("WPA3") -> SecurityType.WPA3
                capabilities.contains("WPA2") && capabilities.contains("WPA") -> SecurityType.WPA_WPA2
                capabilities.contains("WPA2") -> SecurityType.WPA2
                capabilities.contains("WPA") -> SecurityType.WPA
                capabilities.contains("WEP") -> SecurityType.WEP
                capabilities.contains("OWE") -> SecurityType.OWE
                else -> SecurityType.OPEN
            }
        }
    }
}

/**
 * WPS specific information
 */
@Parcelize
data class WpsInfo(
    val isEnabled: Boolean = false,
    val isPbcSupported: Boolean = false,
    val isPinSupported: Boolean = false,
    val isLocked: Boolean = false,
    val configMethods: List<WpsMethod> = emptyList(),
    val deviceName: String? = null,
    val manufacturer: String? = null,
    val modelName: String? = null,
    val modelNumber: String? = null,
    val serialNumber: String? = null,
    // True if this info came from iw binary (accurate)
    val isFromIw: Boolean = false,
) : Parcelable {

    companion object {
        /**
         * Create WpsInfo from Android capabilities string.
         * Note: This only knows WPS is available, but cannot determine
         * the actual supported methods (PBC/PIN) without using iw with root.
         */
        fun fromCapabilities(capabilities: String): WpsInfo {
            val caps = capabilities.uppercase(Locale.ROOT)
            // Only set specific methods if explicitly stated in capabilities
            // Android's capabilities string rarely includes WPS-PBC or WPS-PIN
            val hasPbc = caps.contains("WPS-PBC")
            val hasPin = caps.contains("WPS-PIN")

            // isFromIw = false because this is from capabilities, not iw
            return WpsInfo(
                isEnabled = caps.contains("WPS"),
                isPbcSupported = hasPbc,
                isPinSupported = hasPin,
                isLocked = caps.contains("WPS-LOCKED"),
                configMethods = parseConfigMethods(caps),
                isFromIw = false,
            )
        }

        private fun parseConfigMethods(capabilities: String): List<WpsMethod> {
            val methods = mutableListOf<WpsMethod>()
            if (capabilities.contains("WPS-PBC")) methods.add(WpsMethod.PUSH_BUTTON)
            if (capabilities.contains("WPS-PIN")) methods.add(WpsMethod.PIN)
            if (capabilities.contains("WPS-NFC")) methods.add(WpsMethod.NFC)
            // Don't default to any methods - we can't know without iw
            return methods
        }
    }
}

enum class WpsMethod {
    PUSH_BUTTON,
    PIN,
    NFC,
}

enum class SecurityType {
    OPEN,
    WEP,
    WPA,
    WPA2,
    WPA_WPA2,
    WPA3,
    OWE,
}

enum class WifiBand {
    BAND_2_4_GHZ,
    BAND_5_GHZ,
    BAND_6_GHZ,
    UNKNOWN,
}

enum class SignalStrength {
    EXCELLENT,
    GOOD,
    FAIR,
    WEAK,
}
