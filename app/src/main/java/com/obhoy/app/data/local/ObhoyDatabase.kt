package com.obhoy.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.obhoy.app.data.local.dao.EmergencyContactDao
import com.obhoy.app.data.local.dao.UserProfileDao
import com.obhoy.app.data.local.entity.EmergencyContactEntity
import com.obhoy.app.data.local.entity.UserProfileEntity
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [UserProfileEntity::class, EmergencyContactEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ObhoyDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun emergencyContactDao(): EmergencyContactDao

    companion object {
        @Volatile
        private var INSTANCE: ObhoyDatabase? = null

        fun getInstance(context: Context, passphrase: ByteArray): ObhoyDatabase {
            return INSTANCE ?: synchronized(this) {
                val factory = SupportFactory(passphrase)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ObhoyDatabase::class.java,
                    "obhoy_secure_vault.db"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

