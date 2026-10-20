package com.example.catpet

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** A manually written quick note. Image content is never stored here. */
data class NoteItem(
    val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Local store for text quick notes.
 *
 * Uses its own SharedPreferences file, so it never touches ScreenshotStore data.
 */
class NoteStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun load(): List<NoteItem> = synchronized(lock) {
        val raw = preferences.getString(KEY_ITEMS, null) ?: return@synchronized emptyList()
        try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.optJSONObject(index) ?: continue
                    val id = json.optString(KEY_ID, "")
                    val title = json.optString(KEY_TITLE, "")
                    if (id.isBlank() || title.isBlank()) continue
                    add(
                        NoteItem(
                            id = id,
                            title = title,
                            body = json.optString(KEY_BODY, ""),
                            createdAt = json.optLong(KEY_CREATED_AT, 0L),
                            updatedAt = json.optLong(KEY_UPDATED_AT, 0L)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            preferences.edit().putString(KEY_CORRUPT_BACKUP, raw).apply()
            emptyList()
        }
    }

    fun save(items: List<NoteItem>): Boolean = synchronized(lock) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put(KEY_ID, item.id)
                    .put(KEY_TITLE, item.title)
                    .put(KEY_BODY, item.body)
                    .put(KEY_CREATED_AT, item.createdAt)
                    .put(KEY_UPDATED_AT, item.updatedAt)
            )
        }
        preferences.edit().putString(KEY_ITEMS, array.toString()).commit()
    }

    companion object {
        private const val PREFS_NAME = "quick_note_store"
        private const val KEY_ITEMS = "notes_json"
        private const val KEY_CORRUPT_BACKUP = "notes_json_corrupt_backup"
        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_BODY = "body"
        private const val KEY_CREATED_AT = "createdAt"
        private const val KEY_UPDATED_AT = "updatedAt"
        private val lock = Any()
    }
}
