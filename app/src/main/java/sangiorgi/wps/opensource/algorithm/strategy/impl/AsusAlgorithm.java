package sangiorgi.wps.opensource.algorithm.strategy.impl;

import java.util.Locale;
import sangiorgi.wps.opensource.algorithm.MacAddressUtil;
import sangiorgi.wps.opensource.algorithm.strategy.AlgorithmException;
import sangiorgi.wps.opensource.algorithm.strategy.BaseWpsAlgorithm;

/** ASUS router WPS algorithm implementation */
public class AsusAlgorithm extends BaseWpsAlgorithm {

  public AsusAlgorithm() {
    super("Asus");
  }

  @Override
  public String generatePin(String bssid, String ssid) throws AlgorithmException {
    String[] bytes = MacAddressUtil.splitIntoBytes(bssid);
    int[] byteValues = new int[6];

    for (int i = 0; i < 6; i++) {
      byteValues[i] = parseHexSafe(bytes[i], 0);
    }

    int bhex = byteValues[1] + byteValues[2] + byteValues[3] + byteValues[4] + byteValues[5];
    int[] p = new int[7];

    for (int i = 0; i < 7; i++) {
      p[i] = (byteValues[i % 6] + byteValues[5]) % (10 - ((i + bhex) % 7));
    }

    String pinStr =
        String.format(Locale.ROOT, "%d%d%d%d%d%d%d", p[0], p[1], p[2], p[3], p[4], p[5], p[6]);
    int pin = Integer.parseInt(pinStr);

    return formatPinWithChecksum(pin);
  }
}
