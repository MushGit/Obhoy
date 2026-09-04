package com.obhoy.app.data.repository

import com.obhoy.app.data.local.dao.UserProfileDao
import com.obhoy.app.data.local.entity.UserProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserProfileRepository(
    private val userProfileDao: UserProfileDao
) {

    /**
     * Synchronous blocking getter for direct access on background IO threads (e.g., DispatchManager).
     */
    fun getUserProfileSync(): UserProfileEntity? {
        return userProfileDao.getUserProfileSync()
    }

    suspend fun getUserProfile(): UserProfileEntity? = withContext(Dispatchers.IO) {
        userProfileDao.getUserProfile()
    }

    suspend fun saveUserProfile(profile: UserProfileEntity) = withContext(Dispatchers.IO) {
        userProfileDao.saveUserProfile(profile)
    }

    suspend fun updatePinHashes(truePinHash: String, decoyPinHash: String) = withContext(Dispatchers.IO) {
        val currentProfile = userProfileDao.getUserProfile()
        currentProfile?.let {
            val updated = it.copy(
                truePinHash = truePinHash,
                decoyPinHash = decoyPinHash
            )
            userProfileDao.saveUserProfile(updated)
        }
    }

    suspend fun setOnboardingComplete(isComplete: Boolean) = withContext(Dispatchers.IO) {
        val currentProfile = userProfileDao.getUserProfile()
        currentProfile?.let {
            val updated = it.copy(isSetupComplete = isComplete)
            userProfileDao.saveUserProfile(updated)
        }
    }

    suspend fun clearUserProfile() = withContext(Dispatchers.IO) {
        userProfileDao.clearUserProfile()
    }
}
