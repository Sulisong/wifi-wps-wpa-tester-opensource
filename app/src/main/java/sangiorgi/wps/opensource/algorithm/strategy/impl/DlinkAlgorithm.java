package sangiorgi.wps.opensource.algorithm.strategy.impl;

import java.util.Locale;
import sangiorgi.wps.opensource.algorithm.ChecksumCalculator;
import sangiorgi.wps.opensource.algorithm.MacAddressUtil;
import sangiorgi.wps.opensource.algorithm.strategy.AlgorithmException;
import sangiorgi.wps.opensource.algorithm.strategy.BaseWpsAlgorithm;

/** D-Link router WPS algorithm implementation */
public class DlinkAlgorithm extends BaseWpsAlgorithm {

  private final boolean plusOne;

  public DlinkAlgorithm() {
    this(false);
  }

  public DlinkAlgorithm(boolean plusOne) {
    super(plusOne ? "DLink+1" : "DLink");
    this.plusOne = plusOne;
  }

  @Override
  public String generatePin(String bssid, String ssid) throws AlgorithmException {
    String last3 = MacAddressUtil.getLastThreeBytes(bssid);
    int nic = parseHexSafe(last3, 1234567);

    if (plusOne) {
      nic = (nic + 1) % 100000000;
    } else {
      nic = nic % 100000000;
    }

    int pin = nic ^ 0x55AA55;
    pin =
        pin
            ^ (((pin & 15) << 4)
                + ((pin & 15) << 8)
                + ((pin & 15) << 12)
                + ((pin & 15) << 16)
                + ((pin & 15) << 20));

    pin = pin % PIN_MODULO;
    if (pin < 1000000) {
      pin += ((pin % 9) * 1000000) + 1000000;
    }

    pin = pin * 10;
    return String.format(Locale.ROOT, "%08d", pin + ChecksumCalculator.calculate(pin));
  }
}
