package com.arverse.arbattery.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.arverse.arbattery.core.common.model.ChargingState
import com.arverse.arbattery.core.common.model.ReadingQuality

@Entity(
    tableName = "battery_readings",
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("deviceId"),
        Index("timestamp")
    ]
)
data class BatteryReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String,
    val percentage: Int,
    val chargingState: ChargingState,
    val timestamp: Long,
    val readingSource: String,
    val qualityStatus: ReadingQuality
)
