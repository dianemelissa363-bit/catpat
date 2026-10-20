package com.example.catpet

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore

/**
 * Watches MediaStore for screenshots added while recording mode is on.
 *
 * Best-effort detection: only images that (a) are present in MediaStore.Images,
 * (b) are finalized (IS_PENDING = 0) and (c) look like screenshots by display
 * name or relative path are stored. Each record is deduplicated by MediaStore
 * _ID, and only IDs newer than the moment recording started are considered, so
 * enabling the mode never imports older gallery screenshots.
 *
 * The original image is only referenced by URI. Nothing is copied or deleted.
 */
class ScreenshotWatcher(
    context: Context,
    private val onRecorded: (Int) -> Unit,
    private val onError: (String) -> Unit
) {

    private val appContext = context.applicationContext
    private val resolver: ContentResolver = appContext.contentResolver
    private val store = ScreenshotStore(appContext)
    private var observer: ContentObserver? = null
    private var lastSeenId: Long = 0L
    private var running = false

    fun start(): Boolean {
        if (running) return true
        lastSeenId = currentMaxId()
        val obs = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                scanNew()
            }
        }
        return try {
            resolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                obs
            )
            observer = obs
            running = true
            true
        } catch (_: Exception) {
            stop()
            false
        }
    }

    private fun fail(reason: String) {
        stop()
        onError(reason)
    }

    fun stop() {
        observer?.let {
            try {
                resolver.unregisterContentObserver(it)
            } catch (_: Exception) {
            }
        }
        observer = null
        running = false
    }

    private fun currentMaxId(): Long {
        return try {
            resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                null,
                null,
                "${MediaStore.Images.Media._ID} DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun scanNew() {
        if (!running) return
        val found = mutableListOf<TodoItem>()
        try {
            val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = mutableListOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            )
            val isQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            if (isQ) {
                projection.add(MediaStore.Images.Media.RELATIVE_PATH)
            }
            val selectionParts = mutableListOf("${MediaStore.Images.Media._ID} > ?")
            if (isQ) {
                selectionParts.add("${MediaStore.Images.Media.IS_PENDING} = 0")
            }
            resolver.query(
                collection,
                projection.toTypedArray(),
                selectionParts.joinToString(" AND "),
                arrayOf(lastSeenId.toString()),
                "${MediaStore.Images.Media.DATE_ADDED} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val pathCol = if (isQ) {
                    cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                } else {
                    -1
                }
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    if (id <= lastSeenId) continue
                    lastSeenId = maxOf(lastSeenId, id)
                    val name = cursor.getString(nameCol) ?: ""
                    val path = if (pathCol >= 0) cursor.getString(pathCol) ?: "" else ""
                    if (looksLikeScreenshot(name, path)) {
                        val uri = ContentUris.withAppendedId(collection, id)
                        found.add(
                            TodoItem(
                                mediaId = id,
                                uri = uri.toString(),
                                addedAt = cursor.getLong(dateCol) * 1000L
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Permission revoked or provider failure: stop collecting and let
            // the host clear its state instead of silently pretending to work.
            fail("scan")
            return
        }

        var saved = 0
        found.forEach { if (store.add(it)) saved++ }
        if (saved > 0) {
            onRecorded(saved)
        }
    }

    private fun looksLikeScreenshot(name: String, path: String): Boolean {
        val haystack = (name + " " + path).lowercase()
        return haystack.contains("screenshot") ||
            haystack.contains("screen shot") ||
            haystack.contains("截屏") ||
            haystack.contains("截图")
    }
}
