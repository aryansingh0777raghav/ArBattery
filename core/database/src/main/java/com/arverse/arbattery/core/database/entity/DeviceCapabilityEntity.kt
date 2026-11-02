package com.arverse.arbattery.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_capabilities")
data class DeviceCapabilityEntity(
    @PrimaryKey val deviceId: String,
    val supportedServices: String,
    val batteryServiceAvailable: Boolean,
    val batteryNotificationsAvailable: Boolean,
    val protocolType: String
)
