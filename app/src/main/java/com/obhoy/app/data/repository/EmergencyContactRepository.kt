package com.obhoy.app.data.repository

import com.obhoy.app.data.local.dao.EmergencyContactDao
import com.obhoy.app.data.local.entity.EmergencyContactEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmergencyContactRepository(
    private val emergencyContactDao: EmergencyContactDao
) {

    /**
     * Synchronous blocking getter for direct access on background IO threads (e.g., DispatchManager).
     */
    fun getEmergencyContactsSync(): List<EmergencyContactEntity> {
        return emergencyContactDao.getAllContactsSync()
    }

    suspend fun getAllContacts(): List<EmergencyContactEntity> = withContext(Dispatchers.IO) {
        emergencyContactDao.getAllContacts()
    }

    suspend fun addContact(contact: EmergencyContactEntity) = withContext(Dispatchers.IO) {
        emergencyContactDao.insertContact(contact)
    }

    suspend fun deleteContactById(contactId: Int) = withContext(Dispatchers.IO) {
        emergencyContactDao.deleteContactById(contactId)
    }

    suspend fun clearAllContacts() = withContext(Dispatchers.IO) {
        emergencyContactDao.clearAllContacts()
    }
}
