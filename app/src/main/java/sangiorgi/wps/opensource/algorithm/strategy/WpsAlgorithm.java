package sangiorgi.wps.opensource.algorithm.strategy;

/**
 * Strategy interface for WPS PIN generation algorithms Each algorithm implementation should
 * implement this interface
 */
public interface WpsAlgorithm {

  /**
   * Generates a WPS PIN based on the router's BSSID and SSID
   *
   * @param bssid The router's BSSID (MAC address)
   * @param ssid The router's SSID (network name) - may be null for some algorithms
   * @return The generated WPS PIN as a formatted string
   * @throws AlgorithmException if the PIN cannot be generated
   */
  String generatePin(String bssid, String ssid) throws AlgorithmException;

  /**
   * Gets the name of this algorithm for display purposes
   *
   * @return The algorithm's display name
   */
  String getAlgorithmName();

  /**
   * Validates if the input is suitable for this algorithm
   *
   * @param bssid The router's BSSID
   * @param ssid The router's SSID (may be null)
   * @return true if the input is valid, false otherwise
   */
  boolean validateInput(String bssid, String ssid);
}
