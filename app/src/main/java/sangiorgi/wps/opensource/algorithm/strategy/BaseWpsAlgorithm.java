package sangiorgi.wps.opensource.algorithm.strategy;

import sangiorgi.wps.opensource.algorithm.ChecksumCalculator;
import sangiorgi.wps.opensource.algorithm.MacAddressUtil;

/** Base class for WPS algorithms providing common functionality */
public abstract class BaseWpsAlgorithm implements WpsAlgorithm {

  protected static final String DEFAULT_PIN = "12345670";
  protected static final int PIN_MODULO = 10000000;

  private final String algorithmName;

  protected BaseWpsAlgorithm(String algorithmName) {
    this.algorithmName = algorithmName;
  }

  @Override
  public String getAlgorithmName() {
    return algorithmName;
  }

  @Override
  public boolean validateInput(String bssid, String ssid) {
    return MacAddressUtil.isValidMacAddress(bssid);
  }

  /** Formats a PIN with checksum */
  protected String formatPinWithChecksum(int pin) {
    return ChecksumCalculator.formatWithChecksum(pin);
  }

  /** Formats a PIN with checksum (no algorithm name) */
  protected String formatPin(int pin) {
    return ChecksumCalculator.formatWithChecksum(pin);
  }

  /** Parses hex safely with default value */
  protected int parseHexSafe(String hex, int defaultValue) {
    return MacAddressUtil.parseHexSafe(hex, defaultValue);
  }

  /** Parses long hex safely with default value */
  protected long parseLongHexSafe(String hex, long defaultValue) {
    return MacAddressUtil.parseLongHexSafe(hex, defaultValue);
  }
}
