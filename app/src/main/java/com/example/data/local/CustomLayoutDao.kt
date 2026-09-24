package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for custom controller layout configurations stored in Room.
 */
@Dao
interface CustomLayoutDao {

    @Query("SELECT * FROM custom_controller_layouts ORDER BY lastModified DESC")
    fun getAllLayouts(): Flow<List<CustomLayoutEntity>>

    @Query("SELECT * FROM custom_controller_layouts ORDER BY lastModified DESC")
    suspend fun getAllLayoutsSync(): List<CustomLayoutEntity>

    @Query("SELECT * FROM custom_controller_layouts WHERE id = :id")
    suspend fun getLayoutById(id: String): CustomLayoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayout(layout: CustomLayoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayouts(layouts: List<CustomLayoutEntity>)

    @Update
    suspend fun updateLayout(layout: CustomLayoutEntity)

    @Delete
    suspend fun deleteLayout(layout: CustomLayoutEntity)

    @Query("DELETE FROM custom_controller_layouts WHERE id = :id")
    suspend fun deleteLayoutById(id: String)

    @Query("DELETE FROM custom_controller_layouts")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM custom_controller_layouts")
    suspend fun getLayoutCount(): Int
}
