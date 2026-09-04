package com.obhoy.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val fullName: String,
    val primaryPhone: String,
    val nationalId: String? = null,
    val homeAddress: String? = null,
    val truePinHash: String,
    val decoyPinHash: String,
    val isSetupComplete: Boolean = false
)
