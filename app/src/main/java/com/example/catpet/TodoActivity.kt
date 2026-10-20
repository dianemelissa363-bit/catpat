package com.example.catpet

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Paint
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Standalone "today todo" page.
 *
 * Uses TodoTaskStore only. Unfinished tasks are listed first, finished ones
 * after; both groups are sorted by most recent update. Nothing is cleared by
 * date, so tasks survive day changes. Overdue state is computed at render time
 * from the device clock and is never persisted.
 */
class TodoActivity : Activity() {

    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var store: TodoTaskStore
    private val timeFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo)
        container = findViewById(R.id.tasksContainer)
        tvEmpty = findViewById(R.id.tvEmpty)
        store = TodoTaskStore(this)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnNewTask).setOnClickListener { showTaskDialog(null) }
    }

    override fun onResume() {
        super.onResume()
        // Re-read on every resume so overdue state and external edits refresh.
        renderTasks()
    }

    private fun renderTasks() {
        val tasks = store.load()
        val pending = tasks.filter { !it.done }.sortedByDescending { it.updatedAt }
        val done = tasks.filter { it.done }.sortedByDescending { it.updatedAt }

        container.removeAllViews()
        if (tasks.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            container.visibility = View.GONE
            return
        }
        tvEmpty.visibility = View.GONE
        container.visibility = View.VISIBLE

        if (pending.isNotEmpty()) {
            container.addView(sectionHeader(getString(R.string.todo_section_pending)))
            pending.forEach { container.addView(buildTaskRow(it)) }
        }
        if (done.isNotEmpty()) {
            container.addView(sectionHeader(getString(R.string.todo_section_done)))
            done.forEach { container.addView(buildTaskRow(it)) }
        }
    }

    private fun sectionHeader(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(0xFF666666.toInt())
            setPadding(0, dp(10), 0, dp(4))
        }
    }

    private fun buildTaskRow(task: TodoTask): LinearLayout {
        val now = System.currentTimeMillis()
        val overdue = !task.done && task.dueAt != null && task.dueAt < now

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }

        val check = CheckBox(this).apply {
            isChecked = task.done
            setOnCheckedChangeListener { _, checked ->
                if (checked != task.done) {
                    val updated = task.copy(done = checked, updatedAt = System.currentTimeMillis())
                    store.save(store.load().map { if (it.id == task.id) updated else it })
                    renderTasks()
                }
            }
        }
        row.addView(check)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply { marginStart = dp(8) }
            setOnClickListener { showTaskDialog(task) }
        }
        content.addView(TextView(this).apply {
            text = task.title
            textSize = 16f
            if (task.done) {
                paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            }
            if (overdue) {
                setTextColor(0xFFCC0000.toInt())
            }
        })
        if (task.description.isNotBlank()) {
            content.addView(TextView(this).apply {
                text = task.description
                textSize = 13f
                setTextColor(if (overdue) 0xFFCC0000.toInt() else 0xFF666666.toInt())
                maxLines = 2
            })
        }
        if (task.dueAt != null) {
            content.addView(TextView(this).apply {
                text = getString(R.string.todo_due, timeFormat.format(Date(task.dueAt)))
                textSize = 12f
                setTextColor(if (overdue) 0xFFCC0000.toInt() else 0xFF999999.toInt())
            })
        }
        content.addView(TextView(this).apply {
            text = getString(R.string.todo_item_time, timeFormat.format(Date(task.updatedAt)))
            textSize = 12f
            setTextColor(0xFF999999.toInt())
        })
        row.addView(content)

        row.addView(TextView(this).apply {
            text = getString(R.string.delete)
            textSize = 13f
            setTextColor(0xFFCC3333.toInt())
            setPadding(dp(8), dp(4), 0, dp(4))
            setOnClickListener { confirmDelete(task) }
        })
        return row
    }

    private fun showTaskDialog(task: TodoTask?) {
        var dueAt = task?.dueAt

        val titleInput = EditText(this).apply {
            hint = getString(R.string.todo_title_hint)
            setText(task?.title ?: "")
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val descInput = EditText(this).apply {
            hint = getString(R.string.todo_desc_hint)
            setText(task?.description ?: "")
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
        }
        val dueLabel = TextView(this).apply {
            text = formatDue(dueAt)
            textSize = 13f
            setTextColor(0xFF666666.toInt())
        }
        val dueBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(
                dueLabel,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
            addView(Button(this@TodoActivity).apply {
                text = getString(R.string.todo_set_due)
                setOnClickListener {
                    pickDue(dueAt) { picked ->
                        dueAt = picked
                        dueLabel.text = formatDue(dueAt)
                    }
                }
            })
            addView(Button(this@TodoActivity).apply {
                text = getString(R.string.todo_clear_due)
                setOnClickListener {
                    dueAt = null
                    dueLabel.text = formatDue(dueAt)
                }
            })
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
            addView(titleInput)
            addView(descInput)
            addView(dueBox)
        }
        AlertDialog.Builder(this)
            .setTitle(if (task == null) R.string.todo_dialog_new else R.string.todo_dialog_edit)
            .setView(box)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                val title = titleInput.text.toString().trim()
                if (title.isBlank()) {
                    Toast.makeText(this, R.string.title_required, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val now = System.currentTimeMillis()
                val all = store.load().toMutableList()
                if (task == null) {
                    all.add(
                        TodoTask(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            description = descInput.text.toString().trim(),
                            done = false,
                            createdAt = now,
                            updatedAt = now,
                            dueAt = dueAt
                        )
                    )
                } else {
                    val index = all.indexOfFirst { it.id == task.id }
                    if (index >= 0) {
                        all[index] = task.copy(
                            title = title,
                            description = descInput.text.toString().trim(),
                            updatedAt = now,
                            dueAt = dueAt
                        )
                    }
                }
                if (!store.save(all)) {
                    Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show()
                }
                renderTasks()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun pickDue(current: Long?, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance()
        if (current != null) {
            cal.timeInMillis = current
        }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day)
                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, minute)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        onPicked(cal.timeInMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun formatDue(dueAt: Long?): String {
        return if (dueAt == null) {
            getString(R.string.todo_due_none)
        } else {
            getString(R.string.todo_due, timeFormat.format(Date(dueAt)))
        }
    }

    private fun confirmDelete(task: TodoTask) {
        AlertDialog.Builder(this)
            .setTitle(R.string.todo_delete_title)
            .setMessage(R.string.todo_delete_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                store.save(store.load().filterNot { it.id == task.id })
                renderTasks()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
