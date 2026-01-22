package sangiorgi.wps.opensource.algorithm.strategy.impl;

import sangiorgi.wps.opensource.algorithm.MacAddressUtil;
import sangiorgi.wps.opensource.algorithm.strategy.AlgorithmException;

/** Orange router WPS algorithm implementation */
public class OrangeAlgorithm extends SerialBasedAlgorithm {

  public OrangeAlgorithm(String dataDir) {
    super("Orange", dataDir);
  }

  @Override
  public String generatePin(String bssid, String ssid) throws AlgorithmException {
    String serial = readSerialFromFile(bssid);

    if (serial.length() > 4) {
      serial = serial.substring(serial.length() - 4);
    } else if (serial.length() < 4) {
      throw new AlgorithmException("Serial must be at least 4 characters");
    }

    String wanbssid = MacAddressUtil.getLastTwoBytesWan(bssid);

    int[] shex = new int[4];
    int[] wanhex = new int[4];

    for (int i = 0; i < 4; i++) {
      shex[i] = parseHexSafe(serial.substring(i, i + 1), 0);
      wanhex[i] = parseHexSafe(wanbssid.substring(i, i + 1), 0);
    }

    String checkK1 = Integer.toString(shex[0] + shex[1] + wanhex[2] + wanhex[3], 16);
    String checkK2 = Integer.toString(shex[2] + shex[3] + wanhex[0] + wanhex[1], 16);

    String k1 = checkK1.length() < 2 ? checkK1 : checkK1.substring(1, 2);
    String k2 = checkK2.length() < 2 ? checkK2 : checkK2.substring(1, 2);

    int k1Val = parseHexSafe(k1, 0);
    int k2Val = parseHexSafe(k2, 0);

    String p1 = Integer.toString(shex[3] ^ k1Val, 16);
    String p2 = Integer.toString(shex[2] ^ k1Val, 16);
    String p3 = Integer.toString(wanhex[1] ^ k2Val, 16);
    String p4 = Integer.toString(wanhex[2] ^ k2Val, 16);
    String p5 = Integer.toString(shex[3] ^ wanhex[2], 16);
    String p6 = Integer.toString(shex[2] ^ wanhex[3], 16);
    String p7 = Integer.toString(shex[1] ^ k1Val, 16);

    String hexPin = String.format("%s%s%s%s%s%s%s", p1, p2, p3, p4, p5, p6, p7);
    int pin = parseHexSafe(hexPin, 1234567);

    String prePin = Integer.toString(pin);
    if (prePin.length() > 7) {
      prePin = prePin.substring(prePin.length() - 7);
    }

    int finalPin = Integer.parseInt(prePin);
    return formatPinWithChecksum(finalPin);
  }
}
