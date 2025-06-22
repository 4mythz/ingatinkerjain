package com.example.tugas3kelompok.task

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.tugas3kelompok.R
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private val context: Context,
    private val tasks: MutableList<Task>,
    private val onTaskChecked: (Task) -> Unit,
    private val onTaskEdit: (Task) -> Unit,
    private val onTaskDelete: (Task) -> Unit,
    private val isPreviewMode: Boolean = false
) : BaseAdapter() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    // Definisi warna untuk status tugas
    private val colorOngoing = Color.parseColor("#FFF9C4")    // Kuning muda
    private val colorOverdue = Color.parseColor("#FFCDD2")    // Merah muda
    private val colorCompleted = Color.parseColor("#C8E6C9")  // Hijau muda
    private val warningColor = Color.parseColor("#FFD600") // Kuning warning (untuk text status saja)
    private val overdueColor = Color.parseColor("#FF5252") // Merah (untuk text status saja)
    private val completedColor = Color.parseColor("#43A047") // Hijau (untuk text status saja)
    private val normalColor = Color.parseColor("#FFFFFF") // Putih

    override fun getCount(): Int = tasks.size

    override fun getItem(position: Int): Any = tasks[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_task, parent, false)

        val task = tasks[position]
        val taskContainer = view.findViewById<View>(R.id.taskContainer)
        val taskText = view.findViewById<TextView>(R.id.tvTask)
        val categoryText = view.findViewById<TextView>(R.id.tvCategory)
        val deadlineText = view.findViewById<TextView>(R.id.tvDeadline)
        val statusText = view.findViewById<TextView>(R.id.tvStatus)

        taskText.text = task.text
        categoryText.text = "Kategori: ${task.category}"
        deadlineText.text = "Deadline: ${task.deadline}"

        try {
            val deadlineDate = dateFormat.parse(task.deadline)
            if (deadlineDate != null) {
                val now = Date()
                val diffInMillis = deadlineDate.time - now.time
                val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
                val diffInHours = (diffInMillis / (1000 * 60 * 60)).toInt()
                val diffInMinutes = (diffInMillis / (1000 * 60)).toInt()

                fun setStatusText(label: String, color: Int) {
                    val statusLabel = "Status: "
                    val fullText = statusLabel + label
                    val spannable = SpannableString(fullText)
                    // Warnai 'Status :'
                    spannable.setSpan(
                        ForegroundColorSpan(color),
                        0, statusLabel.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    // Warnai label status
                    spannable.setSpan(
                        ForegroundColorSpan(color),
                        statusLabel.length, fullText.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    statusText.text = spannable
                    statusText.setTypeface(null, android.graphics.Typeface.BOLD)
                }

                if (task.isDone) {
                    // Tugas selesai
                    taskContainer.setBackgroundColor(colorCompleted)
                    setStatusText("Selesai", completedColor)
                } else if (deadlineDate.before(now) || task.isOverdue) {
                    // Tugas terlewat
                    task.isOverdue = true
                    taskContainer.setBackgroundColor(colorOverdue)
                    setStatusText("Terlewat", overdueColor)
                } else {
                    // Tugas masih berlangsung, status detail
                    val status: String
                    val color: Int
                    if (diffInDays < 0) {
                        status = "Terlewat"
                        color = overdueColor
                        taskContainer.setBackgroundColor(colorOverdue)
                    } else if (diffInDays == 0) {
                        if (diffInHours > 0) {
                            status = "$diffInHours jam lagi"
                        } else if (diffInMinutes > 0) {
                            status = "$diffInMinutes menit lagi"
                        } else {
                            status = "Hari ini"
                        }
                        color = if (diffInHours < 72) warningColor else Color.DKGRAY
                        taskContainer.setBackgroundColor(colorOngoing)
                    } else if (diffInDays == 1) {
                        status = "Besok"
                        color = warningColor
                        taskContainer.setBackgroundColor(colorOngoing)
                    } else if (diffInDays <= 3) {
                        status = "Dalam $diffInDays hari"
                        color = warningColor
                        taskContainer.setBackgroundColor(colorOngoing)
                    } else {
                        status = "Dalam $diffInDays hari"
                        color = Color.DKGRAY
                        taskContainer.setBackgroundColor(normalColor)
                    }
                    setStatusText(status, color)
                }
            }
        } catch (e: Exception) {
            statusText.text = ""
            taskContainer.setBackgroundColor(colorOngoing)
        }

        if (task.isDone) {
            taskText.paintFlags = taskText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            taskText.setTextColor(Color.GRAY)
            categoryText.setTextColor(Color.GRAY)
            deadlineText.setTextColor(Color.GRAY)
            statusText.setTextColor(Color.GRAY)
        } else {
            taskText.paintFlags = taskText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            taskText.setTextColor(Color.BLACK)
            categoryText.setTextColor(Color.DKGRAY)
            deadlineText.setTextColor(Color.DKGRAY)
            statusText.setTextColor(Color.DKGRAY)
        }

        if (!isPreviewMode) {
            view.setOnClickListener {
                if (!task.isDone && !task.isOverdue) {
                    onTaskEdit(task)
                }
            }

            view.setOnLongClickListener {
                val options = when {
                    task.isDone || task.isOverdue -> arrayOf("Hapus")
                    else -> arrayOf("Tandai Selesai", "Hapus")
                }

                AlertDialog.Builder(context)
                    .setTitle("Pilih Aksi")
                    .setItems(options) { _, which ->
                        if (task.isDone || task.isOverdue) {
                            onTaskDelete(task)
                        } else {
                            when (which) {
                                0 -> onTaskChecked(task)
                                1 -> onTaskDelete(task)
                            }
                        }
                    }
                    .show()
                true
            }
        } else {
            view.setOnClickListener(null)
            view.setOnLongClickListener(null)
            view.isClickable = false
            view.isLongClickable = false
        }

        return view
    }
}
