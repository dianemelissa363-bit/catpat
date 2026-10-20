package com.example.catpet

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
 * Text notes live in NoteStore and support create/edit/delete. Existing
 * screenshot records keep coming from ScreenshotStore and are shown as a
 * separate section; deleting one only removes this app's record, never the
 * original gallery image.
 */
class QuickNotesActivity : Activity() {

    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var noteStore: NoteStore
    private lateinit var screenshotStore: ScreenshotStore
    private val timeFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

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
            setOnClickListener { openImage(item.uri) }
        }
        content.addView(TextView(this).apply {
            text = getString(R.string.screenshot_record_label)
            textSize = 15f
            setTextColor(0xFF222222.toInt())
        })
        content.addView(TextView(this).apply {
            text = getString(R.string.screenshot_item_time, timeFormat.format(Date(item.addedAt)))
            textSize = 12f
            setTextColor(0xFF999999.toInt())
        })
        row.addView(content)
        row.addView(deleteButton { confirmDeleteScreenshot(item) })
        return row
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
            .setMessage(R.string.note_delete_confirm_screenshot)
            .setPositiveButton(R.string.delete) { _, _ ->
                screenshotStore.remove(item.mediaId)
                renderList()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun openImage(uri: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        } catch (_: Exception) {
            Toast.makeText(this, R.string.image_missing, Toast.LENGTH_SHORT).show()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
