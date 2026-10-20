package com.example.catpet

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Quick notes page.
 *
 * Text notes live in NoteStore and support create/edit/delete. Screenshot
 * records live in ScreenshotStore and support renaming; deleting one asks the
 * user first, then deletes the exact original image through MediaStore and only
 * removes the record after the original was actually deleted.
 */
class QuickNotesActivity : Activity() {

    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var noteStore: NoteStore
    private lateinit var screenshotStore: ScreenshotStore
    private val timeFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    private var pendingDeleteItem: TodoItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_notes)
        container = findViewById(R.id.notesContainer)
        tvEmpty = findViewById(R.id.tvEmpty)
        noteStore = NoteStore(this)
        screenshotStore = ScreenshotStore(this)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnNewNote).setOnClickListener { showNoteDialog(null) }
    }

    override fun onResume() {
        super.onResume()
        renderList()
    }

    private fun renderList() {
        val notes = noteStore.load().sortedByDescending { it.updatedAt }
        val screenshots = screenshotStore.load().sortedByDescending { it.addedAt }

        container.removeAllViews()
        if (notes.isEmpty() && screenshots.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            container.visibility = View.GONE
            return
        }
        tvEmpty.visibility = View.GONE
        container.visibility = View.VISIBLE

        if (notes.isNotEmpty()) {
            container.addView(sectionHeader(getString(R.string.quick_notes_section_text)))
            notes.forEach { container.addView(buildNoteRow(it)) }
        }
        if (screenshots.isNotEmpty()) {
            container.addView(sectionHeader(getString(R.string.quick_notes_section_screenshot)))
            screenshots.forEach { container.addView(buildScreenshotRow(it)) }
        }
    }

    private fun sectionHeader(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(0xFF666666.toInt())
        setPadding(0, dp(10), 0, dp(4))
    }

    private fun buildNoteRow(note: NoteItem): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            setOnClickListener { showNoteDialog(note) }
        }
        content.addView(TextView(this).apply {
            text = note.title
            textSize = 16f
            setTextColor(0xFF222222.toInt())
        })
        if (note.body.isNotBlank()) {
            content.addView(TextView(this).apply {
                text = summary(note.body)
                textSize = 13f
                setTextColor(0xFF666666.toInt())
                maxLines = 2
            })
        }
        content.addView(TextView(this).apply {
            text = getString(R.string.note_item_time, timeFormat.format(Date(note.updatedAt)))
            textSize = 12f
            setTextColor(0xFF999999.toInt())
        })
        row.addView(content)
        row.addView(deleteButton { confirmDeleteNote(note) })
        return row
    }

    private fun buildScreenshotRow(item: TodoItem): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            setOnClickListener { showRenameDialog(item) }
        }
        content.addView(TextView(this).apply {
            text = getString(R.string.screenshot_record_label)
            textSize = 11f
            setTextColor(0xFF999999.toInt())
        })
        content.addView(TextView(this).apply {
            text = screenshotDisplayName(item)
            textSize = 15f
            setTextColor(0xFF222222.toInt())
        })
        content.addView(TextView(this).apply {
            text = getString(R.string.screenshot_item_time, timeFormat.format(Date(item.addedAt)))
            textSize = 12f
            setTextColor(0xFF999999.toInt())
        })
        row.addView(content)
        row.addView(actionButton(getString(R.string.screenshot_view)) { openImage(item.uri) })
        row.addView(deleteButton { confirmDeleteScreenshot(item) })
        return row
    }

    private fun actionButton(label: String, onClick: () -> Unit): TextView = TextView(this).apply {
        text = label
        textSize = 13f
        setTextColor(0xFF3366CC.toInt())
        setPadding(dp(8), dp(4), 0, dp(4))
        setOnClickListener { onClick() }
    }

    private fun deleteButton(onClick: () -> Unit): TextView = TextView(this).apply {
        text = getString(R.string.delete)
        textSize = 13f
        setTextColor(0xFFCC3333.toInt())
        setPadding(dp(8), dp(4), 0, dp(4))
        setOnClickListener { onClick() }
    }

    private fun summary(body: String): String {
        val oneLine = body.replace("\n", " ").trim()
        return if (oneLine.length > 60) oneLine.take(60) + "…" else oneLine
    }

    private fun screenshotDisplayName(item: TodoItem): String {
        if (item.name.isNotBlank()) return item.name
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return getString(R.string.screenshot_default_name, format.format(Date(item.addedAt)))
    }

    private fun showNoteDialog(note: NoteItem?) {
        val titleInput = EditText(this).apply {
            hint = getString(R.string.note_title_hint)
            setText(note?.title ?: "")
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val bodyInput = EditText(this).apply {
            hint = getString(R.string.note_body_hint)
            setText(note?.body ?: "")
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 4
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
            addView(titleInput)
            addView(bodyInput)
        }
        AlertDialog.Builder(this)
            .setTitle(if (note == null) R.string.note_dialog_new else R.string.note_dialog_edit)
            .setView(box)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                val title = titleInput.text.toString().trim()
                if (title.isBlank()) {
                    Toast.makeText(this, R.string.title_required, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val now = System.currentTimeMillis()
                val all = noteStore.load().toMutableList()
                if (note == null) {
                    all.add(
                        NoteItem(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            body = bodyInput.text.toString().trim(),
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                } else {
                    val index = all.indexOfFirst { it.id == note.id }
                    if (index >= 0) {
                        all[index] = note.copy(
                            title = title,
                            body = bodyInput.text.toString().trim(),
                            updatedAt = now
                        )
                    }
                }
                if (!noteStore.save(all)) {
                    Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show()
                }
                renderList()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showRenameDialog(item: TodoItem) {
        val input = EditText(this).apply {
            setText(screenshotDisplayName(item))
            inputType = InputType.TYPE_CLASS_TEXT
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.screenshot_rename_title)
            .setView(input)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isBlank()) {
                    Toast.makeText(this, R.string.title_required, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                screenshotStore.updateName(item.mediaId, name)
                renderList()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun confirmDeleteNote(note: NoteItem) {
        AlertDialog.Builder(this)
            .setTitle(R.string.note_delete_title)
            .setMessage(R.string.note_delete_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                noteStore.save(noteStore.load().filterNot { it.id == note.id })
                renderList()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun confirmDeleteScreenshot(item: TodoItem) {
        AlertDialog.Builder(this)
            .setTitle(R.string.note_delete_title)
            .setMessage(R.string.screenshot_delete_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteOriginalThenRecord(item)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    /**
     * Deletes the exact original image first, and only removes the local record
     * when that actually succeeded. Any failure / denial keeps the record.
     */
    private fun deleteOriginalThenRecord(item: TodoItem) {
        if (item.uri.isBlank()) {
            Toast.makeText(this, R.string.screenshot_delete_no_uri, Toast.LENGTH_LONG).show()
            return
        }
        val uri = Uri.parse(item.uri)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val request = MediaStore.createDeleteRequest(contentResolver, listOf(uri))
                pendingDeleteItem = item
                startIntentSenderForResult(
                    request.intentSender,
                    REQ_DELETE_ORIGINAL,
                    null,
                    0,
                    0,
                    0
                )
            } catch (_: Exception) {
                Toast.makeText(this, R.string.screenshot_delete_failed, Toast.LENGTH_LONG).show()
            }
            return
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            pendingDeleteItem = item
            requestPermissions(
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQ_WRITE_PERMISSION
            )
            return
        }

        try {
            val rows = contentResolver.delete(uri, null, null)
            if (rows > 0) {
                finishDelete(item)
            } else {
                Toast.makeText(this, R.string.screenshot_delete_failed, Toast.LENGTH_LONG).show()
            }
        } catch (e: SecurityException) {
            var handled = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val recoverable = e as? android.app.RecoverableSecurityException
                if (recoverable != null) {
                    try {
                        pendingDeleteItem = item
                        startIntentSenderForResult(
                            recoverable.userAction.actionIntent.intentSender,
                            REQ_DELETE_ORIGINAL,
                            null,
                            0,
                            0,
                            0
                        )
                        handled = true
                    } catch (_: Exception) {
                    }
                }
            }
            if (!handled) {
                Toast.makeText(this, R.string.screenshot_delete_failed, Toast.LENGTH_LONG).show()
            }
        } catch (_: Exception) {
            Toast.makeText(this, R.string.screenshot_delete_failed, Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_DELETE_ORIGINAL) {
            val item = pendingDeleteItem
            pendingDeleteItem = null
            if (resultCode == RESULT_OK && item != null) {
                finishDelete(item)
            } else {
                Toast.makeText(this, R.string.screenshot_delete_cancelled, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_WRITE_PERMISSION) {
            val item = pendingDeleteItem
            pendingDeleteItem = null
            if (item != null &&
                grantResults.isNotEmpty() &&
                grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                deleteOriginalThenRecord(item)
            } else {
                Toast.makeText(this, R.string.screenshot_delete_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Called only after the system confirmed the original image deletion.
     * Removes the record and reports truthfully if the local save failed.
     */
    private fun finishDelete(item: TodoItem) {
        val removed = screenshotStore.remove(item.mediaId)
        renderList()
        if (removed) {
            Toast.makeText(this, R.string.screenshot_delete_ok, Toast.LENGTH_SHORT).show()
            return
        }
        val stillPresent = screenshotStore.load().any { it.mediaId == item.mediaId }
        if (stillPresent) {
            Toast.makeText(this, R.string.screenshot_delete_record_failed, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, R.string.screenshot_delete_ok, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openImage(uri: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        } catch (_: Exception) {
            Toast.makeText(this, R.string.image_missing, Toast.LENGTH_SHORT).show()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQ_DELETE_ORIGINAL = 301
        private const val REQ_WRITE_PERMISSION = 302
    }
}
