# Room Database Preservation
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# SQLCipher Binary & Native Bridge Preservation
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-keepclassmembers class net.sqlcipher.** { *; }

# BCrypt Library Rules
-keep class at.favre.lib.crypto.bcrypt.** { *; }

# Obhoy Database Entities & DAOs
-keep class com.obhoy.app.data.local.entity.** { *; }
-keep class com.obhoy.app.data.local.dao.** { *; }

