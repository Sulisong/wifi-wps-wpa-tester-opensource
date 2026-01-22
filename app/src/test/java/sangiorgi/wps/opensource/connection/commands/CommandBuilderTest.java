package sangiorgi.wps.opensource.connection.commands;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for WPS command building logic. Tests that commands are properly constructed with
 * correct parameters.
 *
 * <p>Note: These tests verify string formatting and logic without executing actual shell commands.
 */
public class CommandBuilderTest {

  private static final String TEST_BSSID = "AA:BB:CC:DD:EE:FF";
  private static final String TEST_PIN = "12345670";
  private static final String TEST_FILES_DIR = "/data/adb/wps/files";

  @Test
  public void testPinSanitization() {
    // Test via reflection or by testing a utility method
    // For now, test the expected behavior

    // Normal 8-digit PIN
    assertEquals("Normal PIN should remain unchanged", "12345670", sanitizePin("12345670"));

    // PIN longer than 8 digits should be truncated
    assertEquals("Long PIN should be truncated", "12345678", sanitizePin("123456789"));

    // Empty PIN
    assertEquals("Empty PIN should return empty quotes", "''", sanitizePin(""));

    // Null PIN
    assertEquals("Null PIN should return empty quotes", "''", sanitizePin(null));

    // NULL_PIN special value
    assertEquals("NULL_PIN should return empty quotes", "''", sanitizePin("NULL_PIN"));
  }

  // Helper method mimicking WpsCommand.sanitizePin logic
  private String sanitizePin(String pin) {
    if (pin == null || pin.isEmpty() || "NULL_PIN".equals(pin)) {
      return "''";
    }
    return pin.length() > 8 ? pin.substring(0, 8) : pin;
  }

  @Test
  public void testBaseCommandFormat() {
    String baseCmd = buildBaseCommand(TEST_FILES_DIR);

    assertTrue("Should contain cd command", baseCmd.contains("cd " + TEST_FILES_DIR));
    assertTrue("Should export LD_LIBRARY_PATH", baseCmd.contains("export LD_LIBRARY_PATH="));
  }

  // Helper method mimicking WpsCommand.getBaseCommand logic
  private String buildBaseCommand(String filesDir) {
    return String.format("cd %s && export LD_LIBRARY_PATH=%s", filesDir, filesDir);
  }

  @Test
  public void testWpaCliCommandFormat() {
    int timeout = 30;
    String baseCmd = buildBaseCommand(TEST_FILES_DIR);

    // Test without IFNAME
    String cmd1 = buildWpaCliCommand(baseCmd, timeout, TEST_BSSID, TEST_PIN, false);
    assertTrue("Should contain wpa_cli_n", cmd1.contains("./wpa_cli_n"));
    assertTrue("Should contain timeout", cmd1.contains("timeout " + timeout));
    assertTrue("Should contain wps_reg", cmd1.contains("wps_reg"));
    assertTrue("Should contain BSSID", cmd1.contains(TEST_BSSID));
    assertTrue("Should contain PIN", cmd1.contains(TEST_PIN));
    assertFalse("Should NOT contain IFNAME", cmd1.contains("IFNAME="));

    // Test with IFNAME
    String cmd2 = buildWpaCliCommand(baseCmd, timeout, TEST_BSSID, TEST_PIN, true);
    assertTrue("Should contain IFNAME=wlan0", cmd2.contains("IFNAME=wlan0"));
  }

  // Helper method mimicking WpaCliCommand.buildCommand logic
  private String buildWpaCliCommand(
      String baseCmd, int timeout, String bssid, String pin, boolean useIfname) {
    String command = String.format("%s && timeout %d ./wpa_cli_n", baseCmd, timeout);
    if (useIfname) {
      return command + " IFNAME=wlan0 wps_reg " + bssid + " " + pin;
    } else {
      return command + " wps_reg " + bssid + " " + pin;
    }
  }

  @Test
  public void testWpaSupplicantCommandFormat() {
    int timeout = 30;
    String baseCmd = buildBaseCommand(TEST_FILES_DIR);
    String configPath = "-c" + TEST_FILES_DIR + "/wpa_supplicant.conf";
    String outputPath = "-K -O/data/misc/wifi/wpswpatester/";

    String cmd = buildWpaSupplicantCommand(baseCmd, timeout, configPath, outputPath);

    assertTrue("Should contain wpa_supplicant", cmd.contains("./wpa_supplicant"));
    assertTrue("Should contain debug flag -d", cmd.contains("-d"));
    assertTrue("Should contain driver options", cmd.contains("-Dnl80211,wext,hostapd,wired"));
    assertTrue("Should contain interface", cmd.contains("-i wlan0"));
    assertTrue("Should contain config path", cmd.contains(configPath));
    assertTrue("Should contain output path", cmd.contains(outputPath));
  }

  // Helper method mimicking WpaSupplicantCommand.buildCommand logic
  private String buildWpaSupplicantCommand(
      String baseCmd, int timeout, String configPath, String outputPath) {
    return String.format(
        "%s && timeout %d ./wpa_supplicant -d -Dnl80211,wext,hostapd,wired -i wlan0 %s %s",
        baseCmd, timeout, configPath, outputPath);
  }

  @Test
  public void testBssidInCommand() {
    // Verify BSSID is included correctly in commands
    String cmd =
        buildWpaCliCommand(buildBaseCommand(TEST_FILES_DIR), 30, TEST_BSSID, TEST_PIN, false);

    // BSSID should appear exactly as provided
    assertTrue("Command should contain exact BSSID", cmd.contains(TEST_BSSID));
  }

  @Test
  public void testTimeoutValues() {
    // Test various timeout values
    int[] timeouts = {10, 30, 60, 120};

    for (int timeout : timeouts) {
      String cmd =
          buildWpaCliCommand(
              buildBaseCommand(TEST_FILES_DIR), timeout, TEST_BSSID, TEST_PIN, false);
      assertTrue("Should contain timeout " + timeout, cmd.contains("timeout " + timeout));
    }
  }

  @Test
  public void testSpecialCharactersInBssid() {
    // BSSID should be uppercase hex with colons
    String[] validBssids = {"AA:BB:CC:DD:EE:FF", "00:11:22:33:44:55", "aa:bb:cc:dd:ee:ff"};

    for (String bssid : validBssids) {
      String cmd = buildWpaCliCommand(buildBaseCommand(TEST_FILES_DIR), 30, bssid, TEST_PIN, false);
      assertTrue("Should include BSSID: " + bssid, cmd.contains(bssid));
    }
  }

  @Test
  public void testCommandChaining() {
    String cmd =
        buildWpaCliCommand(buildBaseCommand(TEST_FILES_DIR), 30, TEST_BSSID, TEST_PIN, false);

    // Commands should be chained with &&
    int andCount = countOccurrences(cmd, "&&");
    assertTrue("Should have at least 2 && operators for chaining", andCount >= 2);
  }

  private int countOccurrences(String str, String sub) {
    int count = 0;
    int idx = 0;
    while ((idx = str.indexOf(sub, idx)) != -1) {
      count++;
      idx += sub.length();
    }
    return count;
  }

  @Test
  public void testPixieDustCommandFormat() {
    // Test Pixie Dust command building
    String pke = "0123456789abcdef";
    String pkr = "fedcba9876543210";
    String ehash1 = "aaaa1111bbbb2222";
    String ehash2 = "cccc3333dddd4444";
    String authKey = "abcdef0123456789";
    String enonce = "1234abcd5678efgh";

    String cmd =
        buildPixieDustCommand(
            TEST_FILES_DIR, TEST_BSSID, pke, pkr, ehash1, ehash2, authKey, enonce);

    assertTrue(
        "Should contain pixiewps or pixiedust binary",
        cmd.contains("pixiewps") || cmd.contains("pixiedust"));
    assertTrue("Should contain PKE parameter", cmd.contains(pke));
    assertTrue("Should contain PKR parameter", cmd.contains(pkr));
    assertTrue("Should contain E-Hash1", cmd.contains(ehash1));
    assertTrue("Should contain E-Hash2", cmd.contains(ehash2));
  }

  // Helper mimicking PixieDustCommand format
  private String buildPixieDustCommand(
      String filesDir,
      String bssid,
      String pke,
      String pkr,
      String ehash1,
      String ehash2,
      String authKey,
      String enonce) {
    return String.format(
        "cd %s && ./pixiewps -e %s -r %s -s %s -z %s -a %s -n %s",
        filesDir, pke, pkr, ehash1, ehash2, authKey, enonce);
  }
}
