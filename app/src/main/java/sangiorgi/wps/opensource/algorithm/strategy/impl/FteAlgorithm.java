package sangiorgi.wps.opensource.algorithm.strategy.impl;

import sangiorgi.wps.opensource.algorithm.MacAddressUtil;
import sangiorgi.wps.opensource.algorithm.strategy.AlgorithmException;
import sangiorgi.wps.opensource.algorithm.strategy.BaseWpsAlgorithm;

/** FTE/Jazzel WPS algorithm implementation */
public class FteAlgorithm extends BaseWpsAlgorithm {

  public FteAlgorithm() {
    super("FTE");
  }

  @Override
  public String generatePin(String bssid, String ssid) throws AlgorithmException {
    if (ssid == null || ssid.length() < 2) {
      throw new AlgorithmException("SSID is required and must be at least 2 characters");
    }

    String bssidFormatted = MacAddressUtil.getLastThreeBytes(bssid).substring(0, 2);
    String ssidFormatted = ssid.substring(ssid.length() - 2);
    String concatenation = bssidFormatted + ssidFormatted;

    int pin = (parseHexSafe(concatenation, 1234567) % PIN_MODULO) + 7;
    return formatPinWithChecksum(pin);
  }

  @Override
  public boolean validateInput(String bssid, String ssid) {
    return super.validateInput(bssid, ssid) && ssid != null && ssid.length() >= 2;
  }
}
