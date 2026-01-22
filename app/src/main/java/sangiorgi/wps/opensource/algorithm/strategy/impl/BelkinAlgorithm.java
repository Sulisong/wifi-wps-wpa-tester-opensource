package sangiorgi.wps.opensource.algorithm.strategy.impl;

import sangiorgi.wps.opensource.algorithm.MacAddressUtil;
import sangiorgi.wps.opensource.algorithm.strategy.AlgorithmException;

/** Belkin router WPS algorithm implementation */
public class BelkinAlgorithm extends SerialBasedAlgorithm {

  public BelkinAlgorithm(String dataDir) {
    super("Belkin", dataDir);
  }

  @Override
  public String generatePin(String bssid, String ssid) throws AlgorithmException {
    String serial = readSerialFromFile(bssid);

    if (serial.length() < 4) {
      throw new AlgorithmException("Serial must be at least 4 characters");
    }

    String normalizedBssid = MacAddressUtil.normalize(bssid);

    // Extract last 4 hex digits from serial and MAC
    int[] s = new int[4];
    int[] n = new int[4];

    for (int i = 0; i < 4; i++) {
      s[3 - i] = parseHexSafe(serial.substring(serial.length() - 1 - i, serial.length() - i), 0);
      n[3 - i] =
          parseHexSafe(
              normalizedBssid.substring(
                  normalizedBssid.length() - 1 - i, normalizedBssid.length() - i),
              0);
    }

    int k1 = (s[2] + s[3] + n[0] + n[1]) % 16;
    int k2 = (s[0] + s[1] + n[3] + n[2]) % 16;

    int pin = k1 ^ s[1];
    int t1 = k1 ^ s[0];
    int t2 = k2 ^ n[1];
    int p1 = n[0] ^ s[1] ^ t1;
    int p2 = k2 ^ n[0] ^ t2;
    int p3 = k1 ^ s[2] ^ k2 ^ n[2];

    k1 = k1 ^ k2;

    pin = ((pin ^ k1) * 16);
    pin = ((pin ^ t1) * 16);
    pin = ((pin ^ p1) * 16);
    pin = ((pin ^ t2) * 16);
    pin = ((pin ^ p2) * 16);
    pin = ((pin ^ k1) * 16);
    pin += p3;

    pin = (pin % PIN_MODULO) - (((pin % PIN_MODULO) / PIN_MODULO) * k1);

    return formatPinWithChecksum(pin);
  }
}
