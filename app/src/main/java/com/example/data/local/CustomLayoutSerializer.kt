package com.example.data.local

import com.example.data.model.ControllerElementId
import com.example.data.model.ElementLayoutConfig
import com.example.data.model.StickStylePreset
import org.json.JSONArray
import org.json.JSONObject

/**
 * Robust JSON serializer and deserializer for controller layout elements stored in Room.
 */
object CustomLayoutSerializer {

    fun serialize(elements: Map<ControllerElementId, ElementLayoutConfig>): String {
        val array = JSONArray()
        elements.values.forEach { config ->
            val obj = JSONObject().apply {
                put("elementId", config.elementId.name)
                put("xPercent", config.xPercent.toDouble())
                put("yPercent", config.yPercent.toDouble())
                put("scale", config.scale.toDouble())
                put("stylePreset", config.stylePreset.name)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun deserialize(json: String): Map<ControllerElementId, ElementLayoutConfig> {
        val map = mutableMapOf<ControllerElementId, ElementLayoutConfig>()
        if (json.isBlank()) return map
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val elementId = try {
                    ControllerElementId.valueOf(obj.getString("elementId"))
                } catch (e: Exception) {
                    continue
                }
                val stylePreset = try {
                    StickStylePreset.valueOf(obj.optString("stylePreset", "HALO"))
                } catch (e: Exception) {
                    StickStylePreset.HALO
                }
                map[elementId] = ElementLayoutConfig(
                    elementId = elementId,
                    xPercent = obj.optDouble("xPercent", 50.0).toFloat(),
                    yPercent = obj.optDouble("yPercent", 50.0).toFloat(),
                    scale = obj.optDouble("scale", 1.0).toFloat(),
                    stylePreset = stylePreset
                )
            }
        } catch (e: Exception) {
            // Return whatever was successfully parsed or empty
        }
        return map
    }
}
