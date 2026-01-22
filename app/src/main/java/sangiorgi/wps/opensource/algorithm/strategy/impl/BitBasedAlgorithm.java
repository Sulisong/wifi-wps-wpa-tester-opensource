package sangiorgi.wps.opensource.algorithm.strategy.impl;

import sangiorgi.wps.opensource.algorithm.MacAddressUtil;
import sangiorgi.wps.opensource.algorithm.strategy.AlgorithmException;
import sangiorgi.wps.opensource.algorithm.strategy.BaseWpsAlgorithm;

/** Base class for bit-based algorithms (28, 32, 36, 40, 44, 48 bit) */
public class BitBasedAlgorithm extends BaseWpsAlgorithm {

  public enum BitType {
    TWENTY_EIGHT(28, 5, 12),
    THIRTY_TWO(32, 4, 12),
    THIRTY_SIX(36, 3, 12),
    FORTY(40, 2, 12),
    FORTY_FOUR(44, 1, 12),
    FORTY_EIGHT(48, 0, 12);

    private final int bits;
    private final int startIndex;
    private final int endIndex;

    BitType(int bits, int startIndex, int endIndex) {
      this.bits = bits;
      this.startIndex = startIndex;
      this.endIndex = endIndex;
    }
  }

  private final BitType bitType;

  public BitBasedAlgorithm(BitType bitType) {
    super(bitType.bits + "-bit");
    this.bitType = bitType;
  }

  @Override
  public String generatePin(String bssid, String ssid) throws AlgorithmException {
    String normalized = MacAddressUtil.normalize(bssid);

    if (bitType == BitType.THIRTY_TWO) {
      // Special case for 32-bit
      int bhex = parseHexSafe(normalized.substring(5, 12), 0) % PIN_MODULO;
      int pin = (parseHexSafe(normalized.substring(4, 5), 0) * 8435456 + bhex) % PIN_MODULO;
      return formatPinWithChecksum(pin);
    } else {
      // All other bit-based algorithms
      long pin =
          parseLongHexSafe(normalized.substring(bitType.startIndex, bitType.endIndex), 1234567L)
              % PIN_MODULO;
      return formatPinWithChecksum((int) pin);
    }
  }
}
