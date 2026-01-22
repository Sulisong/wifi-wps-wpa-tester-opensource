package sangiorgi.wps.opensource.algorithm.strategy.impl;

import java.util.Locale;
import sangiorgi.wps.opensource.algorithm.ChecksumCalculator;
import sangiorgi.wps.opensource.algorithm.MacAddressUtil;
import sangiorgi.wps.opensource.algorithm.strategy.AlgorithmException;
import sangiorgi.wps.opensource.algorithm.strategy.BaseWpsAlgorithm;

/** Standard 24-bit PIN algorithm */
public class PinAlgorithm extends BaseWpsAlgorithm {

  public PinAlgorithm() {
    super("24-bit");
  }

  @Override
  public String generatePin(String bssid, String ssid) throws AlgorithmException {
    String last3 = MacAddressUtil.getLastThreeBytes(bssid);
    int value = parseHexSafe(last3, 1234567) % PIN_MODULO;
    int pin = value * 10;
    return String.format(Locale.ROOT, "%08d", pin + ChecksumCalculator.calculate(pin));
  }
}
