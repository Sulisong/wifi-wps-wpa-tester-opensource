package sangiorgi.wps.opensource.algorithm.strategy.impl;

import java.util.Locale;
import sangiorgi.wps.opensource.algorithm.ChecksumCalculator;
import sangiorgi.wps.opensource.algorithm.MacAddressUtil;
import sangiorgi.wps.opensource.algorithm.strategy.AlgorithmException;
import sangiorgi.wps.opensource.algorithm.strategy.BaseWpsAlgorithm;

/** TrendNet router WPS algorithm implementation */
public class TrendNetAlgorithm extends BaseWpsAlgorithm {

  public TrendNetAlgorithm() {
    super("TrendNet");
  }

  @Override
  public String generatePin(String bssid, String ssid) throws AlgorithmException {
    String last3 = MacAddressUtil.getLastThreeBytes(bssid);
    String reversed = last3.substring(4) + last3.substring(2, 4) + last3.substring(0, 2);

    int value = parseHexSafe(reversed, 1234567) % PIN_MODULO;
    int pin = value * 10;

    return String.format(Locale.ROOT, "%08d", pin + ChecksumCalculator.calculate(pin));
  }
}
