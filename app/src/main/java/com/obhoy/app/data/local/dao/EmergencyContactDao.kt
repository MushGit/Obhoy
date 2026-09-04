package com.obhoy.app.data.local.dao

import androidx.room.*
import com.obhoy.app.data.local.entity.EmergencyContactEntity

@Dao
interface EmergencyContactDao {
    @Query("SELECT * FROM emergency_contacts ORDER BY priorityOrder ASC")
    suspend fun getAllContacts(): List<EmergencyContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContactEntity)

    @Query("DELETE FROM emergency_contacts WHERE id = :contactId")
    suspend fun deleteContactById(contactId: Int)

    @Query("DELETE FROM emergency_contacts")
    suspend fun clearAllContacts()
}
