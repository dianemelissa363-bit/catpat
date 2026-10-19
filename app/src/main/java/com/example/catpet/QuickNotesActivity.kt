package com.example.catpet

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full list page for quick notes.
 *
 * Reads the existing ScreenshotStore data as-is (no migration, no deletion of
 * any stored record or original image) and shows it newest-first.
 */
class QuickNotesActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_notes)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        val container = findViewById<LinearLayout>(R.id.notesContainer)
        val emptyView = findViewById<TextView>(R.id.tvEmpty)
        val items = ScreenshotStore(this).load().sortedByDescending { it.addedAt }

        if (items.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            container.visibility = View.GONE
            return
        }

        emptyView.visibility = View.GONE
        container.visibility = View.VISIBLE

        val timeFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        items.forEach { item ->
            val row = TextView(this).apply {
                text = getString(R.string.todo_item_title, item.mediaId) +
                    "\n" + getString(
                        R.string.todo_item_time,
                        timeFormat.format(Date(item.addedAt))
                    )
                textSize = 15f
                setPadding(dp(12), dp(12), dp(12), dp(12))
            }
            container.addView(row)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
