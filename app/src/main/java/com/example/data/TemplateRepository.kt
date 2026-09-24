package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CannedTemplate
import com.example.model.TemplateCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "canned_sms_preferences")

class TemplateRepository(private val context: Context) {

    private val templatesKey = stringPreferencesKey("saved_templates_json")
    private val drivingModeKey = booleanPreferencesKey("driving_mode_active")

    val templatesFlow: Flow<List<CannedTemplate>> = context.dataStore.data.map { preferences ->
        val rawJson = preferences[templatesKey]
        if (rawJson.isNullOrBlank()) {
            CannedTemplate.DEFAULT_TEMPLATES
        } else {
            try {
                val jsonArray = JSONArray(rawJson)
                val list = mutableListOf<CannedTemplate>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(CannedTemplate.fromJsonObject(obj))
                }
                if (list.isEmpty()) {
                    CannedTemplate.DEFAULT_TEMPLATES
                } else {
                    list
                }
            } catch (e: Exception) {
                CannedTemplate.DEFAULT_TEMPLATES
            }
        }
    }

    val drivingModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[drivingModeKey] ?: false
    }

    private fun serializeTemplates(templates: List<CannedTemplate>): String {
        val jsonArray = JSONArray()
        templates.forEach { template ->
            jsonArray.put(template.toJsonObject())
        }
        return jsonArray.toString()
    }

    suspend fun saveTemplates(templates: List<CannedTemplate>) {
        context.dataStore.edit { preferences ->
            preferences[templatesKey] = serializeTemplates(templates)
        }
    }

    suspend fun addTemplate(text: String, category: TemplateCategory): CannedTemplate {
        val newTemplate = CannedTemplate(
            text = text.trim(),
            category = category,
            createdAt = System.currentTimeMillis()
        )
        context.dataStore.edit { preferences ->
            val raw = preferences[templatesKey]
            val currentList = if (raw.isNullOrBlank()) {
                CannedTemplate.DEFAULT_TEMPLATES.toMutableList()
            } else {
                try {
                    val arr = JSONArray(raw)
                    val list = mutableListOf<CannedTemplate>()
                    for (i in 0 until arr.length()) {
                        list.add(CannedTemplate.fromJsonObject(arr.getJSONObject(i)))
                    }
                    list
                } catch (e: Exception) {
                    CannedTemplate.DEFAULT_TEMPLATES.toMutableList()
                }
            }
            // Add new template at the beginning
            currentList.add(0, newTemplate)
            preferences[templatesKey] = serializeTemplates(currentList)
        }
        return newTemplate
    }

    suspend fun updateTemplate(updated: CannedTemplate) {
        context.dataStore.edit { preferences ->
            val raw = preferences[templatesKey] ?: return@edit
            try {
                val arr = JSONArray(raw)
                val list = mutableListOf<CannedTemplate>()
                for (i in 0 until arr.length()) {
                    val item = CannedTemplate.fromJsonObject(arr.getJSONObject(i))
                    if (item.id == updated.id) {
                        list.add(updated)
                    } else {
                        list.add(item)
                    }
                }
                preferences[templatesKey] = serializeTemplates(list)
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
    }

    suspend fun deleteTemplate(id: String): CannedTemplate? {
        var removedItem: CannedTemplate? = null
        context.dataStore.edit { preferences ->
            val raw = preferences[templatesKey]
            val list = if (raw.isNullOrBlank()) {
                CannedTemplate.DEFAULT_TEMPLATES.toMutableList()
            } else {
                try {
                    val arr = JSONArray(raw)
                    val items = mutableListOf<CannedTemplate>()
                    for (i in 0 until arr.length()) {
                        items.add(CannedTemplate.fromJsonObject(arr.getJSONObject(i)))
                    }
                    items
                } catch (e: Exception) {
                    CannedTemplate.DEFAULT_TEMPLATES.toMutableList()
                }
            }
            val index = list.indexOfFirst { it.id == id }
            if (index != -1) {
                removedItem = list.removeAt(index)
                preferences[templatesKey] = serializeTemplates(list)
            }
        }
        return removedItem
    }

    suspend fun restoreTemplate(template: CannedTemplate, atIndex: Int = 0) {
        context.dataStore.edit { preferences ->
            val raw = preferences[templatesKey]
            val list = if (raw.isNullOrBlank()) {
                CannedTemplate.DEFAULT_TEMPLATES.toMutableList()
            } else {
                try {
                    val arr = JSONArray(raw)
                    val items = mutableListOf<CannedTemplate>()
                    for (i in 0 until arr.length()) {
                        items.add(CannedTemplate.fromJsonObject(arr.getJSONObject(i)))
                    }
                    items
                } catch (e: Exception) {
                    CannedTemplate.DEFAULT_TEMPLATES.toMutableList()
                }
            }
            val safeIndex = atIndex.coerceIn(0, list.size)
            list.add(safeIndex, template)
            preferences[templatesKey] = serializeTemplates(list)
        }
    }

    suspend fun incrementUsage(id: String) {
        context.dataStore.edit { preferences ->
            val raw = preferences[templatesKey]
            val list = if (raw.isNullOrBlank()) {
                CannedTemplate.DEFAULT_TEMPLATES.toMutableList()
            } else {
                try {
                    val arr = JSONArray(raw)
                    val items = mutableListOf<CannedTemplate>()
                    for (i in 0 until arr.length()) {
                        items.add(CannedTemplate.fromJsonObject(arr.getJSONObject(i)))
                    }
                    items
                } catch (e: Exception) {
                    CannedTemplate.DEFAULT_TEMPLATES.toMutableList()
                }
            }
            val index = list.indexOfFirst { it.id == id }
            if (index != -1) {
                val current = list[index]
                list[index] = current.copy(usageCount = current.usageCount + 1)
                preferences[templatesKey] = serializeTemplates(list)
            }
        }
    }

    suspend fun togglePin(id: String) {
        context.dataStore.edit { preferences ->
            val raw = preferences[templatesKey]
            val list = if (raw.isNullOrBlank()) {
                CannedTemplate.DEFAULT_TEMPLATES.toMutableList()
            } else {
                try {
                    val arr = JSONArray(raw)
                    val items = mutableListOf<CannedTemplate>()
                    for (i in 0 until arr.length()) {
                        items.add(CannedTemplate.fromJsonObject(arr.getJSONObject(i)))
                    }
                    items
                } catch (e: Exception) {
                    CannedTemplate.DEFAULT_TEMPLATES.toMutableList()
                }
            }
            val index = list.indexOfFirst { it.id == id }
            if (index != -1) {
                val current = list[index]
                list[index] = current.copy(isPinned = !current.isPinned)
                preferences[templatesKey] = serializeTemplates(list)
            }
        }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences[templatesKey] = serializeTemplates(CannedTemplate.DEFAULT_TEMPLATES)
        }
    }

    suspend fun setDrivingMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[drivingModeKey] = enabled
        }
    }
}
