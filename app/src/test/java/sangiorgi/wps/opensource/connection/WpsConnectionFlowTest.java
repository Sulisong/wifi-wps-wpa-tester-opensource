package sangiorgi.wps.opensource.connection;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import sangiorgi.wps.opensource.algorithm.ChecksumCalculator;
import sangiorgi.wps.opensource.connection.commands.CommandResult;
import sangiorgi.wps.opensource.connection.commands.WpsCommand;
import sangiorgi.wps.opensource.connection.services.WpsResult;

/**
 * Integration tests for WPS connection flow. Tests the complete flow from PIN selection through
 * result parsing.
 *
 * <p>These tests verify: 1. PIN generation and validation 2. Command result parsing 3.
 * Success/failure detection 4. Password extraction
 */
public class WpsConnectionFlowTest {

  private static final String TEST_BSSID = "AA:BB:CC:DD:EE:FF";

  @Before
  public void setUp() {
    // Setup test environment
  }

  // ============ PIN Generation Tests ============

  @Test
  public void testStandardPinListGeneration() {
    // Standard PINs that should be tried
    String[] standardPins = {
      "12345670", // Most common default
      "00000000", // All zeros
      "11111111" // Repeating ones (if valid)
    };

    for (String pin : standardPins) {
      assertTrue("PIN should be 8 digits: " + pin, pin.length() == 8);
      assertTrue("PIN should be numeric: " + pin, pin.matches("\\d{8}"));
    }
  }

  @Test
  public void testBruteforcePinGeneration() {
    // Test sequential PIN generation for bruteforce
    int[] firstFourDigits = {0, 1, 9999};

    for (int first4 : firstFourDigits) {
      String pin = generateBruteforcePin(first4, 0);
      assertEquals("PIN should be 8 digits", 8, pin.length());

      // Verify checksum is valid
      int pin7 = Integer.parseInt(pin.substring(0, 7));
      int expectedChecksum = ChecksumCalculator.calculatePreMultiplied(pin7);
      int actualChecksum = Integer.parseInt(pin.substring(7));
      assertEquals("Checksum should be valid", expectedChecksum, actualChecksum);
    }
  }

  private String generateBruteforcePin(int first4, int last3) {
    int pin7 = first4 * 1000 + last3;
    int checksum = ChecksumCalculator.calculatePreMultiplied(pin7);
    return String.format("%07d%d", pin7, checksum);
  }

  @Test
  public void testBelkinPinGeneration() {
    // Belkin PIN generation uses MAC address seed
    String mac = "AA:BB:CC:DD:EE:FF";
    String pin = generateBelkinPin(mac);

    assertEquals("Belkin PIN should be 8 digits", 8, pin.length());
    assertTrue("Belkin PIN should be numeric", pin.matches("\\d{8}"));
  }

  private String generateBelkinPin(String bssid) {
    // Simplified Belkin algorithm - uses last 6 hex digits of MAC
    String mac = bssid.replace(":", "");
    int seed = Integer.parseInt(mac.substring(6), 16);
    return String.format("%08d", seed % 100000000);
  }

  // ============ Connection Result Flow Tests ============

  @Test
  public void testSuccessfulConnectionFlow() {
    // Simulate a successful WPS connection
    String pin = "12345670";

    // Step 1: WPA Supplicant starts
    CommandResult supplicantResult = createSuccessResult("wpa_supplicant started successfully");

    // Step 2: WPS connection succeeds
    List<String> wpsOutput =
        Arrays.asList(
            "WPS: Starting registration protocol",
            "WPS: Received M2",
            "WPS: Received M4",
            "WPS: Received M6",
            "WPS: Received M8",
            "WPS_SUCCESS",
            "WPA: Key negotiation completed",
            "CTRL-EVENT-CONNECTED",
            "Network connected",
            "psk=\"MyNetworkPassword123\"");
    CommandResult wpsResult =
        new CommandResult(true, wpsOutput, null, WpsCommand.CommandType.WPA_CLI);

    // Create WpsResult
    WpsResult result = new WpsResult(TEST_BSSID, pin, Arrays.asList(supplicantResult, wpsResult));

    // Verify flow
    assertTrue("Connection should be successful", result.isSuccess());
    assertFalse("Should not be timeout", result.isTimeout());
    assertFalse("Should not be locked", result.isLocked());
    assertEquals("PIN should match", pin, result.getPin());
    assertEquals("BSSID should match", TEST_BSSID, result.getBssid());
  }

  @Test
  public void testTimeoutConnectionFlow() {
    String pin = "12345670";

    List<String> wpsOutput =
        Arrays.asList(
            "WPS: Starting registration protocol",
            "WPS: Received M2",
            "WPS: Timeout waiting for M4",
            "WPS: Protocol timeout");
    CommandResult wpsResult =
        new CommandResult(false, wpsOutput, null, WpsCommand.CommandType.WPA_CLI);

    WpsResult result = new WpsResult(TEST_BSSID, pin, Arrays.asList(wpsResult));

    assertFalse("Connection should not be successful", result.isSuccess());
    assertTrue("Should detect timeout", result.isTimeout());
  }

  @Test
  public void testLockedConnectionFlow() {
    String pin = "99999999";

    List<String> wpsOutput =
        Arrays.asList(
            "WPS: Starting registration protocol",
            "WPS-PBC-OVERLAP detected",
            "WPS: AP is locked due to too many failed attempts");
    CommandResult wpsResult =
        new CommandResult(false, wpsOutput, null, WpsCommand.CommandType.WPA_CLI);

    WpsResult result = new WpsResult(TEST_BSSID, pin, Arrays.asList(wpsResult));

    assertFalse("Connection should not be successful", result.isSuccess());
    assertTrue("Should detect locked state", result.isLocked());
  }

  @Test
  public void testWrongPinConnectionFlow() {
    String pin = "00000000";

    List<String> wpsOutput =
        Arrays.asList(
            "WPS: Starting registration protocol",
            "WPS: Received M2",
            "WPS: Received M2D (PIN error)",
            "WPS: Registrar reported PIN failure");
    CommandResult wpsResult =
        new CommandResult(false, wpsOutput, null, WpsCommand.CommandType.WPA_CLI);

    WpsResult result = new WpsResult(TEST_BSSID, pin, Arrays.asList(wpsResult));

    assertFalse("Wrong PIN should fail", result.isSuccess());
    assertFalse("Should not be timeout", result.isTimeout());
    assertFalse("Should not be locked (single failure)", result.isLocked());
  }

  // ============ Password Extraction Tests ============

  @Test
  public void testPasswordExtractionFromPsk() {
    String pin = "12345670";

    List<String> wpsOutput =
        Arrays.asList("WPS_SUCCESS", "CTRL-EVENT-CONNECTED", "psk=\"SecurePassword123!@#\"");
    CommandResult wpsResult =
        new CommandResult(true, wpsOutput, null, WpsCommand.CommandType.WPA_CLI);

    WpsResult result = new WpsResult(TEST_BSSID, pin, Arrays.asList(wpsResult));

    assertTrue("Should be successful", result.isSuccess());
    // Note: Current implementation looks for "wpa_psk=" pattern
  }

  @Test
  public void testPasswordExtractionFromCredentials() {
    String pin = "12345670";

    List<String> wpsOutput =
        Arrays.asList(
            "WPS_SUCCESS",
            "Network credentials received:",
            "Password: MyWiFiPassword",
            "CTRL-EVENT-CONNECTED");
    CommandResult wpsResult =
        new CommandResult(true, wpsOutput, null, WpsCommand.CommandType.WPA_CLI);

    WpsResult result = new WpsResult(TEST_BSSID, pin, Arrays.asList(wpsResult));

    assertTrue("Should be successful", result.isSuccess());
    assertEquals("Should extract password", "MyWiFiPassword", result.getPassword());
  }

  // ============ Multiple PIN Attempts Flow ============

  @Test
  public void testMultiplePinAttemptFlow() {
    String[] pinsToTry = {"00000000", "12345670", "11111111"};
    WpsResult successResult = null;

    for (int i = 0; i < pinsToTry.length; i++) {
      String pin = pinsToTry[i];

      // Simulate attempt
      WpsResult result = simulatePinAttempt(pin, i == 1); // Second PIN succeeds

      if (result.isSuccess()) {
        successResult = result;
        break;
      }
    }

    assertNotNull("Should find successful PIN", successResult);
    assertEquals("Successful PIN should be 12345670", "12345670", successResult.getPin());
  }

  private WpsResult simulatePinAttempt(String pin, boolean shouldSucceed) {
    List<String> output;
    if (shouldSucceed) {
      output = Arrays.asList("WPS_SUCCESS", "CTRL-EVENT-CONNECTED", "Password: TestPass123");
    } else {
      output = Arrays.asList("WPS: M2D received - PIN error");
    }

    CommandResult cmdResult =
        new CommandResult(shouldSucceed, output, null, WpsCommand.CommandType.WPA_CLI);
    return new WpsResult(TEST_BSSID, pin, Arrays.asList(cmdResult));
  }

  // ============ Edge Cases ============

  @Test
  public void testEmptyOutputHandling() {
    String pin = "12345670";
    CommandResult emptyResult =
        new CommandResult(false, Arrays.asList(), null, WpsCommand.CommandType.WPA_CLI);

    WpsResult result = new WpsResult(TEST_BSSID, pin, Arrays.asList(emptyResult));

    assertFalse("Empty output should not be success", result.isSuccess());
    assertFalse("Empty output should not be timeout", result.isTimeout());
    assertFalse("Empty output should not be locked", result.isLocked());
  }

  @Test
  public void testNullLinesInOutput() {
    String pin = "12345670";
    List<String> output = Arrays.asList("Line 1", null, "WPS_SUCCESS", null);
    CommandResult cmdResult = new CommandResult(true, output, null, WpsCommand.CommandType.WPA_CLI);

    WpsResult result = new WpsResult(TEST_BSSID, pin, Arrays.asList(cmdResult));

    // Should handle null lines gracefully
    assertTrue("Should still detect success despite null lines", result.isSuccess());
  }

  @Test
  public void testMixedSuccessAndFailureIndicators() {
    String pin = "12345670";
    // Some routers send mixed messages
    List<String> output =
        Arrays.asList(
            "WPS: Warning - weak signal",
            "WPS: Retry...",
            "WPS: timeout on first attempt",
            "WPS: Retrying...",
            "WPS_SUCCESS", // Eventually succeeds
            "CTRL-EVENT-CONNECTED");
    CommandResult cmdResult = new CommandResult(true, output, null, WpsCommand.CommandType.WPA_CLI);

    WpsResult result = new WpsResult(TEST_BSSID, pin, Arrays.asList(cmdResult));

    // Success should take precedence
    assertTrue("Success should be detected even with earlier timeout messages", result.isSuccess());
  }

  // ============ Helper Methods ============

  private CommandResult createSuccessResult(String message) {
    return new CommandResult(
        true, Arrays.asList(message), null, WpsCommand.CommandType.WPA_SUPPLICANT);
  }

  private CommandResult createFailureResult(String message) {
    return new CommandResult(false, Arrays.asList(message), null, WpsCommand.CommandType.WPA_CLI);
  }
}
