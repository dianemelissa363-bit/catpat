package com.example.catpet

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** A todo task. Kept independently from screenshots and text notes. */
data class TodoTask(
    val id: String,
    val title: String,
    val description: String,
    val done: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Local store for todo tasks.
 *
 * Uses its own SharedPreferences file and never clears data by date: unfinished
 * and finished tasks both survive day changes until the user deletes them.
 */
class TodoTaskStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun load(): List<TodoTask> = synchronized(lock) {
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
                        TodoTask(
                            id = id,
                            title = title,
                            description = json.optString(KEY_DESC, ""),
                            done = json.optBoolean(KEY_DONE, false),
                            createdAt = json.optLong(KEY_CREATED_AT, 0L),
                            updatedAt = json.optLong(KEY_UPDATED_AT, 0L)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(items: List<TodoTask>): Boolean = synchronized(lock) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put(KEY_ID, item.id)
                    .put(KEY_TITLE, item.title)
                    .put(KEY_DESC, item.description)
                    .put(KEY_DONE, item.done)
                    .put(KEY_CREATED_AT, item.createdAt)
                    .put(KEY_UPDATED_AT, item.updatedAt)
            )
        }
        preferences.edit().putString(KEY_ITEMS, array.toString()).commit()
    }

    companion object {
        private const val PREFS_NAME = "todo_task_store"
        private const val KEY_ITEMS = "tasks_json"
        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_DESC = "description"
        private const val KEY_DONE = "done"
        private const val KEY_CREATED_AT = "createdAt"
        private const val KEY_UPDATED_AT = "updatedAt"
        private val lock = Any()
    }
}
