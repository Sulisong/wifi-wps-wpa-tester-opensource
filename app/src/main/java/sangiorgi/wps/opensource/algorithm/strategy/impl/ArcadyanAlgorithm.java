package sangiorgi.wps.opensource.algorithm.strategy.impl;

import java.util.Locale;
import sangiorgi.wps.opensource.algorithm.MacAddressUtil;
import sangiorgi.wps.opensource.algorithm.strategy.AlgorithmException;
import sangiorgi.wps.opensource.algorithm.strategy.BaseWpsAlgorithm;

/** Arcadyan/EasyBox WPS algorithm implementation */
public class ArcadyanAlgorithm extends BaseWpsAlgorithm {

  public ArcadyanAlgorithm() {
    super("EasyBox");
  }

  @Override
  public String generatePin(String bssid, String ssid) throws AlgorithmException {
    String lastThree = MacAddressUtil.getLastThreeBytes(bssid);
    String lastTwo = lastThree.substring(2, 6);

    int sn = parseHexSafe(lastTwo, 0);
    String snStr = String.format(Locale.ROOT, "%05d", sn);

    // Extract individual digits and MAC nibbles
    int[] snDigits = new int[4];
    int[] macNibbles = new int[4];

    for (int i = 0; i < 4; i++) {
      snDigits[i] = Character.getNumericValue(snStr.charAt(i + 1));
      macNibbles[i] = parseHexSafe(lastTwo.substring(i, i + 1), 0);
    }

    // Calculate K1 and K2
    int k1 = (snDigits[0] + snDigits[1] + macNibbles[2] + macNibbles[3]) % 16;
    int k2 = (snDigits[2] + snDigits[3] + macNibbles[0] + macNibbles[1]) % 16;

    // Calculate PIN digits
    int[] pinDigits = new int[7];
    pinDigits[0] = k1 ^ snDigits[3];
    pinDigits[1] = k1 ^ snDigits[2];
    pinDigits[2] = k2 ^ macNibbles[1];
    pinDigits[3] = k2 ^ macNibbles[2];
    pinDigits[4] = macNibbles[2] ^ snDigits[3];
    pinDigits[5] = macNibbles[3] ^ snDigits[2];
    pinDigits[6] = k1 ^ snDigits[1];

    String hexPin =
        String.format(
            "%X%X%X%X%X%X%X",
            pinDigits[0],
            pinDigits[1],
            pinDigits[2],
            pinDigits[3],
            pinDigits[4],
            pinDigits[5],
            pinDigits[6]);

    int pin = parseHexSafe(hexPin, 1234567) % PIN_MODULO;
    return formatPinWithChecksum(pin);
  }
}
