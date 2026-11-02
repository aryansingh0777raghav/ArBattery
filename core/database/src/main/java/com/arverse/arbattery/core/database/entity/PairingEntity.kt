package com.arverse.arbattery.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pairings")
data class PairingEntity(
    @PrimaryKey val deviceId: String,
    val tokenHash: String,
    val pairedAt: Long = System.currentTimeMillis(),
    val lastSync: Long = System.currentTimeMillis(),
    val status: String = "ACTIVE"
)
