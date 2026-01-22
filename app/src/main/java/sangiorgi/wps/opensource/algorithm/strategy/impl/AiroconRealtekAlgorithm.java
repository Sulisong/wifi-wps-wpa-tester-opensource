package sangiorgi.wps.opensource.algorithm.strategy.impl;

import java.util.Locale;
import sangiorgi.wps.opensource.algorithm.MacAddressUtil;
import sangiorgi.wps.opensource.algorithm.strategy.AlgorithmException;
import sangiorgi.wps.opensource.algorithm.strategy.BaseWpsAlgorithm;

/** Airocon/Realtek WPS algorithm implementation */
public class AiroconRealtekAlgorithm extends BaseWpsAlgorithm {

  public AiroconRealtekAlgorithm() {
    super("Airocon");
  }

  @Override
  public String generatePin(String bssid, String ssid) throws AlgorithmException {
    String[] bytes = MacAddressUtil.splitIntoBytes(bssid);
    int[] p = new int[7];

    for (int i = 0; i < 6; i++) {
      int b1 = parseHexSafe(bytes[i], 0);
      int b2 = parseHexSafe(bytes[(i + 1) % 6], 0);
      p[i] = (b1 + b2) % 10;
    }
    p[6] = p[0];

    String pinStr =
        String.format(Locale.ROOT, "%d%d%d%d%d%d%d", p[0], p[1], p[2], p[3], p[4], p[5], p[6]);
    int pin = Integer.parseInt(pinStr);

    return formatPinWithChecksum(pin);
  }
}
