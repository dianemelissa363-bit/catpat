package com.example.catpet

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject

data class TodoItem(
    val mediaId: Long,
    val uri: String,
    val addedAt: Long,
    val name: String = ""
)

/**
 * Lightweight local store for screenshot records.
 *
 * A record only references the original image by MediaStore id/URI. Removing a
 * record here never touches the gallery; deleting the original is handled by
 * the UI through MediaStore after explicit user confirmation.
 *
 * Old JSON without "name" / "uri" is upgraded in memory: a missing URI is
 * rebuilt from the stable media id (never guessed), and a missing name simply
 * falls back to a display default.
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
     * Removes only the local record. Callers are responsible for deciding
     * whether the original image was already deleted successfully.
     */
    fun remove(mediaId: Long): Boolean = synchronized(lock) {
        val items = loadLocked()
        val updated = items.filterNot { it.mediaId == mediaId }
        if (updated.size == items.size) {
            return@synchronized false
        }
        saveLocked(updated)
    }

    /** Persists an edited display name without touching the original image. */
    fun updateName(mediaId: Long, name: String): Boolean = synchronized(lock) {
        val items = loadLocked()
        val updated = items.map { if (it.mediaId == mediaId) it.copy(name = name) else it }
        if (updated == items) {
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
                    if (mediaId < 0L) continue
                    val storedUri = json.optString(KEY_URI, "")
                    val uri = if (storedUri.isNotBlank()) {
                        storedUri
                    } else {
                        // Old record without a URI: rebuild it from the stable id.
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            mediaId
                        ).toString()
                    }
                    add(
                        TodoItem(
                            mediaId = mediaId,
                            uri = uri,
                            addedAt = json.optLong(KEY_ADDED_AT, 0L),
                            name = json.optString(KEY_NAME, "")
                        )
                    )
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
                    .put(KEY_NAME, item.name)
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
        private const val KEY_NAME = "name"
        private const val INVALID_ID = -1L
        private const val EMPTY_ARRAY = "[]"
        private val lock = Any()
    }
}
