package sangiorgi.wps.opensource.connection.services;

import javax.inject.Inject;
import javax.inject.Singleton;
import sangiorgi.wps.opensource.connection.utils.PinUtils;

@Singleton
public class PinValidationService {

  private final PinUtils pinUtils;

  @Inject
  public PinValidationService(PinUtils pinUtils) {
    this.pinUtils = pinUtils;
  }

  public boolean isPinAlreadyTested(String bssid, String pin) {
    if (pin == null || pin.length() < 8) {
      return false;
    }

    String testPin = pin.substring(0, 8);
    return pinUtils.isPinAlreadyTested(bssid, testPin);
  }

  public void storePinResult(String bssid, String pin, boolean success) {
    pinUtils.storePinResult(bssid, pin, success, null);
  }

  /**
   * Store a failed PIN with indication that first 4 digits were correct. This stores the PIN with
   * "last_three" suffix so we know to only vary the last 3 digits for future attempts.
   */
  public void storeFirstHalfCorrect(String bssid, String pin) {
    pinUtils.storePinResult(bssid, pin, false, "last_three");
  }
}
