package com.example.catpet

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class TodoItem(
    val mediaId: Long,
    val uri: String,
    val addedAt: Long
)

/**
 * Lightweight local store for screenshot todo records.
 *
 * The image itself is never copied or deleted here. Removing a TodoItem only
 * removes its reference from this app's preferences.
 */
class ScreenshotStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun load(): List<TodoItem> = synchronized(lock) {
        loadLocked()
    }

    fun contains(mediaId: Long): Boolean = synchronized(lock) {
        loadLocked().any { it.mediaId == mediaId }
    }

    /**
     * Adds a record if its MediaStore ID is not already present.
     * Returns false for a duplicate or if preferences could not be saved.
     */
    fun add(item: TodoItem): Boolean = synchronized(lock) {
        val items = loadLocked()
        if (items.any { it.mediaId == item.mediaId }) {
            return@synchronized false
        }
        saveLocked(items + item)
    }

    /**
     * Removes only the local todo record. It never deletes the original image.
     */
    fun remove(mediaId: Long): Boolean = synchronized(lock) {
        val items = loadLocked()
        val updated = items.filterNot { it.mediaId == mediaId }
        if (updated.size == items.size) {
            return@synchronized false
        }
        saveLocked(updated)
    }

    private fun loadLocked(): List<TodoItem> {
        val raw = preferences.getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.optJSONObject(index) ?: continue
                    val mediaId = json.optLong(KEY_MEDIA_ID, INVALID_ID)
                    val uri = json.optString(KEY_URI, "")
                    val addedAt = json.optLong(KEY_ADDED_AT, 0L)
                    if (mediaId >= 0L && uri.isNotBlank()) {
                        add(TodoItem(mediaId, uri, addedAt))
                    }
                }
            }
        } catch (_: Exception) {
            // Preserve the malformed value for diagnosis, then recover to an empty store.
            preferences.edit()
                .putString(KEY_CORRUPT_BACKUP, raw)
                .putString(KEY_ITEMS, EMPTY_ARRAY)
                .commit()
            emptyList()
        }
    }

    private fun saveLocked(items: List<TodoItem>): Boolean {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put(KEY_MEDIA_ID, item.mediaId)
                    .put(KEY_URI, item.uri)
                    .put(KEY_ADDED_AT, item.addedAt)
            )
        }
        return preferences.edit().putString(KEY_ITEMS, array.toString()).commit()
    }

    companion object {
        private const val PREFS_NAME = "screenshot_todo_store"
        private const val KEY_ITEMS = "items_json"
        private const val KEY_CORRUPT_BACKUP = "items_json_corrupt_backup"
        private const val KEY_MEDIA_ID = "mediaId"
        private const val KEY_URI = "uri"
        private const val KEY_ADDED_AT = "addedAt"
        private const val INVALID_ID = -1L
        private const val EMPTY_ARRAY = "[]"
        private val lock = Any()
    }
}
