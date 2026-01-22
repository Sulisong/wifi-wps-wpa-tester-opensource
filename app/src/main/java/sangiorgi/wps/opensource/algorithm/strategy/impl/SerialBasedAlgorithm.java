package sangiorgi.wps.opensource.algorithm.strategy.impl;

import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import sangiorgi.wps.opensource.algorithm.strategy.AlgorithmException;
import sangiorgi.wps.opensource.algorithm.strategy.BaseWpsAlgorithm;

/** Base class for algorithms that require serial number files */
public abstract class SerialBasedAlgorithm extends BaseWpsAlgorithm {

  private static final String TAG = "SerialBasedAlgorithm";
  protected final String dataDir;

  protected SerialBasedAlgorithm(String algorithmName, String dataDir) {
    super(algorithmName);
    this.dataDir = dataDir;
  }

  /** Reads serial from file */
  protected String readSerialFromFile(String bssid) throws AlgorithmException {
    String serialFile = dataDir + bssid + "serial";
    File file = new File(serialFile);

    if (!file.exists()) {
      throw new AlgorithmException("Serial file not found: " + serialFile);
    }

    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      String serial = br.readLine();
      if (serial == null || serial.trim().isEmpty()) {
        throw new AlgorithmException("Serial file is empty");
      }
      return serial.trim();
    } catch (IOException e) {
      Log.e(TAG, "Error reading serial file", e);
      throw new AlgorithmException("Failed to read serial file", e);
    }
  }

  @Override
  public boolean validateInput(String bssid, String ssid) {
    if (!super.validateInput(bssid, ssid)) {
      return false;
    }

    // Check if serial file exists
    String serialFile = dataDir + bssid + "serial";
    return new File(serialFile).exists();
  }
}
