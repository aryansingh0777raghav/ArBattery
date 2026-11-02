package com.arverse.arbattery.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.arverse.arbattery.core.database.converter.DatabaseConverters
import com.arverse.arbattery.core.database.dao.BatteryReadingDao
import com.arverse.arbattery.core.database.dao.DeviceCapabilityDao
import com.arverse.arbattery.core.database.dao.DeviceDao
import com.arverse.arbattery.core.database.dao.PairingDao
import com.arverse.arbattery.core.database.entity.BatteryReadingEntity
import com.arverse.arbattery.core.database.entity.DeviceCapabilityEntity
import com.arverse.arbattery.core.database.entity.DeviceEntity
import com.arverse.arbattery.core.database.entity.PairingEntity

@Database(
    entities = [
        DeviceEntity::class,
        BatteryReadingEntity::class,
        PairingEntity::class,
        DeviceCapabilityEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class ArBatteryDatabase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao
    abstract fun batteryReadingDao(): BatteryReadingDao
    abstract fun pairingDao(): PairingDao
    abstract fun deviceCapabilityDao(): DeviceCapabilityDao

    companion object {
        private const val DATABASE_NAME = "arbattery_database.db"

        @Volatile
        private var INSTANCE: ArBatteryDatabase? = null

        fun getInstance(context: Context): ArBatteryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ArBatteryDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun createInMemoryDatabase(context: Context): ArBatteryDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                ArBatteryDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
