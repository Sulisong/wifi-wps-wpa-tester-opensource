package sangiorgi.wps.opensource.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sangiorgi.wps.opensource.data.assets.WpaToolsInitializer
import sangiorgi.wps.opensource.data.assets.WpaToolsPaths
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VendorDatabaseHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "VendorDatabaseHelper"
        private const val TABLE_NAME = "oui"
        private const val COLUMN_MAC = "mac"
        private const val COLUMN_VENDOR = "vendor"
    }

    private var database: SQLiteDatabase? = null
    private val databasePath: String = WpaToolsPaths(context).getVendorDatabasePath()

    init {
        // Wait for initialization to complete
        WpaToolsInitializer.waitForInitialization()
        openDatabase()
    }

    private fun openDatabase() {
        try {
            database = SQLiteDatabase.openDatabase(
                databasePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
            Log.d(TAG, "Database opened successfully from: $databasePath")
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error opening database at: $databasePath", e)
        }
    }

    suspend fun getVendorByMac(macAddress: String): String = withContext(Dispatchers.IO) {
        if (database == null || !database!!.isOpen) {
            openDatabase()
        }

        val normalizedMac = normalizeMacAddress(macAddress)
        val macPrefix = extractMacPrefix(normalizedMac)

        try {
            database?.let { db ->
                val cursor = db.rawQuery(
                    "SELECT $COLUMN_VENDOR FROM $TABLE_NAME WHERE $COLUMN_MAC = ? LIMIT 1",
                    arrayOf(macPrefix),
                )

                cursor.use {
                    if (it.moveToFirst()) {
                        it.getString(0) ?: "Unknown"
                    } else {
                        "Unknown"
                    }
                }
            } ?: "Unknown"
        } catch (e: Exception) {
            Log.e(TAG, "Error querying vendor for MAC: $macAddress", e)
            "Unknown"
        }
    }

    private fun normalizeMacAddress(mac: String): String {
        return mac.uppercase().replace(":", "").replace("-", "")
    }

    private fun extractMacPrefix(normalizedMac: String): String {
        return if (normalizedMac.length >= 6) {
            normalizedMac.substring(0, 6)
        } else {
            normalizedMac
        }
    }
}
