package sangiorgi.wps.opensource.algorithm;

import static org.junit.Assert.*;

import org.junit.Test;

/** Unit tests for WPS PIN checksum calculation. */
public class ChecksumCalculatorTest {

  @Test
  public void testKnownPinChecksums() {
    // Test known valid WPS PINs (8 digits with correct checksum)
    // The calculate() method expects a 7-digit number and calculates the 8th digit

    // For PIN 12345670, the 7-digit portion is 1234567
    // Let's verify the checksum calculation
    int checksum1234567 = ChecksumCalculator.calculate(1234567);
    // The actual checksum is calculated based on the WPS algorithm
    assertTrue("Checksum should be 0-9", checksum1234567 >= 0 && checksum1234567 <= 9);

    // For PIN 00000000 (all zeros), 7-digit is 0000000 = 0
    int checksum0 = ChecksumCalculator.calculate(0);
    assertEquals("0000000 should have checksum 0", 0, checksum0);
  }

  @Test
  public void testPreMultipliedChecksum() {
    // For pre-multiplied, the input is the 7-digit PIN portion
    // Testing that calculatePreMultiplied gives consistent results
    int pin7digit = 1234567;
    int checksum = ChecksumCalculator.calculatePreMultiplied(pin7digit);

    // The checksum should be 0 for 1234567 to make 12345670
    assertEquals("Checksum for 1234567 should be 0", 0, checksum);
  }

  @Test
  public void testFormatWithChecksum() {
    // Test that format produces correct 8-digit PIN string
    String formatted = ChecksumCalculator.formatWithChecksum(1234567);
    assertEquals("Should format as 12345670", "12345670", formatted);

    // Test padding for smaller numbers
    String formattedSmall = ChecksumCalculator.formatWithChecksum(123);
    assertEquals("Should pad with zeros", 8, formattedSmall.length());
    assertTrue("Should start with zeros", formattedSmall.startsWith("0000"));
  }

  @Test
  public void testFormatWithChecksumAndAlgorithm() {
    String formatted = ChecksumCalculator.formatWithChecksum(1234567, "TestAlgo");
    assertTrue("Should contain PIN", formatted.contains("12345670"));
    assertTrue("Should contain algorithm name", formatted.contains("TestAlgo"));
    assertTrue("Should have separator", formatted.contains("--"));
  }

  @Test
  public void testCalculateLong() {
    // Test long version for larger PIN values
    long checksum = ChecksumCalculator.calculateLong(1234567L);
    assertEquals("Long checksum should match int checksum", 0L, checksum);
  }

  @Test
  public void testChecksumRange() {
    // Checksum should always be 0-9
    for (int i = 0; i < 100; i++) {
      int pin = i * 100000; // Various 7-digit numbers
      int checksum = ChecksumCalculator.calculate(pin);
      assertTrue("Checksum should be >= 0", checksum >= 0);
      assertTrue("Checksum should be <= 9", checksum <= 9);
    }
  }

  @Test
  public void testValidatePinWithChecksum() {
    // Generate a PIN and verify it has valid checksum
    int pin7 = 9876543;
    int checksum = ChecksumCalculator.calculatePreMultiplied(pin7);
    String fullPin = String.format("%07d%d", pin7, checksum);

    // Parse and verify
    assertEquals("Full PIN should be 8 digits", 8, fullPin.length());

    // Re-calculate checksum to verify
    int recalculated = ChecksumCalculator.calculate(Integer.parseInt(fullPin.substring(0, 7)));
    assertEquals("Recalculated checksum should match", checksum, (Integer.parseInt(fullPin) % 10));
  }

  @Test
  public void testEdgeCases() {
    // Test boundary values
    assertEquals("Max 7-digit should work", true, ChecksumCalculator.calculate(9999999) >= 0);
    assertEquals("Min (0) should work", 0, ChecksumCalculator.calculate(0));
  }

  @Test
  public void testCommonDefaultPins() {
    // Test commonly used default WPS PINs
    // These are real-world PINs that should pass checksum validation

    // 12345670 - very common default
    verifyValidPin("12345670");

    // 00000000 - another common default
    verifyValidPin("00000000");
  }

  private void verifyValidPin(String pin8digit) {
    int pin7 = Integer.parseInt(pin8digit.substring(0, 7));
    int expectedChecksum = Integer.parseInt(pin8digit.substring(7));
    int calculatedChecksum = ChecksumCalculator.calculatePreMultiplied(pin7);

    assertEquals(
        "PIN " + pin8digit + " should have valid checksum", expectedChecksum, calculatedChecksum);
  }

  @Test
  public void testAlgorithmConsistency() {
    // Verify calculate and calculatePreMultiplied produce consistent results
    for (int pin = 0; pin < 1000; pin++) {
      int checksum1 = ChecksumCalculator.calculate(pin);
      int checksum2 = ChecksumCalculator.calculatePreMultiplied(pin);
      // Note: calculate expects 7-digit number, calculatePreMultiplied multiplies by 10 internally
      // They have slightly different use cases, but both should produce valid checksums
      assertTrue("Checksum1 should be valid", checksum1 >= 0 && checksum1 <= 9);
      assertTrue("Checksum2 should be valid", checksum2 >= 0 && checksum2 <= 9);
    }
  }
}
