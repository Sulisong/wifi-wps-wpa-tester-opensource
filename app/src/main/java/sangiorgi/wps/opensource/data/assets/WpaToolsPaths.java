package sangiorgi.wps.opensource.data.assets;

import android.content.Context;
import java.io.File;

public class WpaToolsPaths {
  private final File filesDir;

  public WpaToolsPaths(Context context) {
    this.filesDir = context.getFilesDir();
  }

  public String getVendorDatabasePath() {
    return new File(filesDir, "vendor.db").getAbsolutePath();
  }

  public String getPinDatabasePath() {
    return new File(filesDir, "pin.db").getAbsolutePath();
  }
}
