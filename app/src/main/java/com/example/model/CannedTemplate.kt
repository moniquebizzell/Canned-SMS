package com.example.model

import org.json.JSONObject
import java.util.UUID

enum class TemplateCategory(val displayName: String, val iconName: String) {
    ALL("All", "Apps"),
    TRANSIT("Transit", "DirectionsCar"),
    ARRIVAL("Arrival", "LocationOn"),
    BUSY("Busy", "AccessTime"),
    CUSTOM("Custom", "EditNote")
}

data class CannedTemplate(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val category: TemplateCategory = TemplateCategory.CUSTOM,
    val usageCount: Int = 0,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("text", text)
            put("category", category.name)
            put("usageCount", usageCount)
            put("isPinned", isPinned)
            put("createdAt", createdAt)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): CannedTemplate {
            val categoryStr = json.optString("category", TemplateCategory.CUSTOM.name)
            val category = try {
                TemplateCategory.valueOf(categoryStr)
            } catch (e: Exception) {
                TemplateCategory.CUSTOM
            }

            return CannedTemplate(
                id = json.optString("id", UUID.randomUUID().toString()),
                text = json.optString("text", ""),
                category = category,
                usageCount = json.optInt("usageCount", 0),
                isPinned = json.optBoolean("isPinned", false),
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        }

        // Required defaults by user prompt:
        // 1. "I'm running about 5 minutes late!"
        // 2. "I have arrived and am outside."
        // 3. "In a meeting, can I call you back soon?"
        val DEFAULT_TEMPLATES = listOf(
            CannedTemplate(
                id = "default-transit-1",
                text = "I'm running about 5 minutes late!",
                category = TemplateCategory.TRANSIT,
                usageCount = 5,
                isPinned = true,
                createdAt = 1000L
            ),
            CannedTemplate(
                id = "default-arrival-1",
                text = "I have arrived and am outside.",
                category = TemplateCategory.ARRIVAL,
                usageCount = 4,
                isPinned = true,
                createdAt = 2000L
            ),
            CannedTemplate(
                id = "default-busy-1",
                text = "In a meeting, can I call you back soon?",
                category = TemplateCategory.BUSY,
                usageCount = 3,
                isPinned = true,
                createdAt = 3000L
            ),
            CannedTemplate(
                id = "default-transit-2",
                text = "On my way now, see you shortly!",
                category = TemplateCategory.TRANSIT,
                usageCount = 2,
                isPinned = false,
                createdAt = 4000L
            ),
            CannedTemplate(
                id = "default-busy-2",
                text = "Driving right now. Will respond when safely parked.",
                category = TemplateCategory.BUSY,
                usageCount = 2,
                isPinned = false,
                createdAt = 5000L
            ),
            CannedTemplate(
                id = "default-arrival-2",
                text = "Just pulled into the parking lot. Which entrance?",
                category = TemplateCategory.ARRIVAL,
                usageCount = 1,
                isPinned = false,
                createdAt = 6000L
            )
        )
    }
}
