package com.example.data.local

import com.example.data.model.ControllerLayoutProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository layer for custom controller layout configurations stored in Room.
 * Implements the Room Database Integration skill architecture mandates.
 */
class CustomLayoutRepository(
    private val customLayoutDao: CustomLayoutDao
) {

    /**
     * Observable reactive stream of all saved controller layout configurations.
     */
    val allLayouts: Flow<List<CustomLayoutEntity>> = customLayoutDao.getAllLayouts()

    suspend fun getAllLayoutsSync(): List<CustomLayoutEntity> {
        return customLayoutDao.getAllLayoutsSync()
    }

    suspend fun getLayoutById(id: String): CustomLayoutEntity? {
        return customLayoutDao.getLayoutById(id)
    }

    suspend fun saveLayout(layout: CustomLayoutEntity) {
        customLayoutDao.insertLayout(layout)
    }

    suspend fun saveLayouts(layouts: List<CustomLayoutEntity>) {
        customLayoutDao.insertLayouts(layouts)
    }

    suspend fun deleteLayoutById(id: String) {
        customLayoutDao.deleteLayoutById(id)
    }

    suspend fun deleteLayout(layout: CustomLayoutEntity) {
        customLayoutDao.deleteLayout(layout)
    }

    suspend fun updateLayout(layout: CustomLayoutEntity) {
        customLayoutDao.updateLayout(layout)
    }

    suspend fun getLayoutCount(): Int {
        return customLayoutDao.getLayoutCount()
    }

    /**
     * Seeds initial default configurations if the database is empty.
     */
    suspend fun seedInitialPresetsIfEmpty() {
        if (customLayoutDao.getLayoutCount() == 0) {
            val presets = listOf(
                CustomLayoutEntity(
                    id = "preset_default",
                    name = "Default Asymmetric",
                    description = "Standard balanced layout with offset sticks and classic face cluster",
                    isPreset = true,
                    elementCount = ControllerLayoutProfile.defaultLayoutElements().size,
                    elementsJson = CustomLayoutSerializer.serialize(ControllerLayoutProfile.defaultLayoutElements()),
                    lastModified = System.currentTimeMillis() - 4000000
                ),
                CustomLayoutEntity(
                    id = "preset_fps",
                    name = "FPS Pro Tournament",
                    description = "Elevated triggers, enlarged sticks, rear paddles P1/P2 for instant crouch & jump",
                    isPreset = true,
                    elementCount = ControllerLayoutProfile.fpsLayoutElements().size,
                    elementsJson = CustomLayoutSerializer.serialize(ControllerLayoutProfile.fpsLayoutElements()),
                    lastModified = System.currentTimeMillis() - 3000000
                ),
                CustomLayoutEntity(
                    id = "preset_fighting",
                    name = "Arcade Fighter",
                    description = "Clustered action buttons, rapid turbo button, and oversized directional D-pad",
                    isPreset = true,
                    elementCount = ControllerLayoutProfile.arcadeFightingLayoutElements().size,
                    elementsJson = CustomLayoutSerializer.serialize(ControllerLayoutProfile.arcadeFightingLayoutElements()),
                    lastModified = System.currentTimeMillis() - 2000000
                ),
                CustomLayoutEntity(
                    id = "preset_southpaw",
                    name = "Southpaw Tactical",
                    description = "Left & Right stick positions inverted for left-handed aim dominance",
                    isPreset = true,
                    elementCount = ControllerLayoutProfile.southpawLayoutElements().size,
                    elementsJson = CustomLayoutSerializer.serialize(ControllerLayoutProfile.southpawLayoutElements()),
                    lastModified = System.currentTimeMillis() - 1000000
                )
            )
            customLayoutDao.insertLayouts(presets)
        }
    }
}
