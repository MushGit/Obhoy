package com.obhoy.app

import android.app.Application
import com.obhoy.app.data.local.ObhoyDatabase
import net.sqlcipher.database.SQLiteDatabase

class ObhoyApplication : Application() {

    lateinit var database: ObhoyDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Load native SQLCipher libraries
        SQLiteDatabase.loadLibs(this)

        // Generate or retrieve persistent hardware passphrase (derived locally)
        val passphrase = getOrCreateLocalPassphrase()
        database = ObhoyDatabase.getInstance(this, passphrase)
    }

    private fun getOrCreateLocalPassphrase(): ByteArray {
        val prefs = getSharedPreferences("obhoy_key_store", MODE_PRIVATE)
        var key = prefs.getString("db_key", null)
        if (key == null) {
            val randomBytes = ByteArray(32)
            java.security.SecureRandom().nextBytes(randomBytes)
            key = android.util.Base64.encodeToString(randomBytes, android.util.Base64.DEFAULT)
            prefs.edit().putString("db_key", key).apply()
        }
        return android.util.Base64.decode(key, android.util.Base64.DEFAULT)
    }
}

