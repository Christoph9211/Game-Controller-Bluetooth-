package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "layout_configs")
data class LayoutConfigEntity(
    @PrimaryKey val elementIdString: String,
    val xPercent: Float,
    val yPercent: Float,
    val scale: Float,
    val stylePresetString: String
)

@Dao
interface LayoutConfigDao {
    @Query("SELECT * FROM layout_configs")
    fun getAllConfigs(): Flow<List<LayoutConfigEntity>>

    @Query("SELECT * FROM layout_configs")
    suspend fun getAllConfigsSync(): List<LayoutConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfigs(configs: List<LayoutConfigEntity>)

    @Query("DELETE FROM layout_configs")
    suspend fun clearAll()
}

@Entity(tableName = "paired_devices")
data class PairedDeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subtitle: String,
    val type: String,
    val connectionType: String,
    val rssiDbm: Int,
    val isPaired: Boolean,
    val isConnected: Boolean,
    val streamRate: String,
    val lastSeenOrConnected: String
)

@Dao
interface PairedDeviceDao {
    @Query("SELECT * FROM paired_devices")
    fun getAllDevices(): Flow<List<PairedDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: PairedDeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<PairedDeviceEntity>)

    @Query("UPDATE paired_devices SET isConnected = :connected WHERE id = :id")
    suspend fun updateConnection(id: String, connected: Boolean)

    @Query("UPDATE paired_devices SET isConnected = 0")
    suspend fun disconnectAll()
}

@Database(entities = [LayoutConfigEntity::class, PairedDeviceEntity::class], version = 1, exportSchema = false)
abstract class GamepadDatabase : RoomDatabase() {
    abstract fun layoutDao(): LayoutConfigDao
    abstract fun deviceDao(): PairedDeviceDao
}
