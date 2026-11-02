package com.arverse.arbattery.core.bluetooth.model

import kotlinx.serialization.Serializable

@Serializable
data class GattDescriptorInfo(
    val uuid: String,
    val permissions: Int
)

@Serializable
data class GattCharacteristicInfo(
    val uuid: String,
    val properties: Int,
    val propertyNames: List<String>,
    val canRead: Boolean,
    val canNotify: Boolean,
    val canIndicate: Boolean,
    val canWrite: Boolean,
    val descriptors: List<GattDescriptorInfo> = emptyList(),
    val lastReadHex: String? = null,
    val lastReadString: String? = null
)

@Serializable
data class GattServiceInfo(
    val uuid: String,
    val serviceName: String,
    val isPrimary: Boolean,
    val characteristics: List<GattCharacteristicInfo>
)

@Serializable
data class GattProfile(
    val deviceAddress: String,
    val deviceName: String,
    val services: List<GattServiceInfo>,
    val hasBatteryService: Boolean,
    val hasBatteryLevelCharacteristic: Boolean,
    val discoveredAt: Long = System.currentTimeMillis()
)
