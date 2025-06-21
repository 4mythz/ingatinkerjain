package com.example.tugas3kelompok

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tugas3kelompok.utils.DeadlineNotificationScheduler
import java.text.SimpleDateFormat
import java.util.*

class TaskFormActivity : AppCompatActivity() {
    private lateinit var etTaskTitle: EditText
    private lateinit var etTaskContent: EditText
    private lateinit var btnNext: Button
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private lateinit var notificationScheduler: DeadlineNotificationScheduler
    private var editingTaskId: Int = -1
    private var isEditing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_form)

        notificationScheduler = DeadlineNotificationScheduler(this)

        etTaskTitle = findViewById(R.id.etTaskTitle)
        etTaskContent = findViewById(R.id.etTaskContent)
        btnNext = findViewById(R.id.btnNext)

        // Get task data if editing
        editingTaskId = intent.getIntExtra("task_id", -1)
        isEditing = intent.getBooleanExtra("is_editing", false)
        val taskText = intent.getStringExtra("task_text")
        val taskContent = intent.getStringExtra("task_content") ?: ""
        val taskDeadline = intent.getStringExtra("task_deadline")
        val position = intent.getIntExtra("task_position", -1)

        if (taskText != null) {
            // Split task text into title and content if it contains a delimiter
            val parts = taskText.split(" - ", limit = 2)
            etTaskTitle.setText(parts[0])
            if (parts.size > 1) {
                etTaskContent.setText(parts[1])
            } else {
                etTaskContent.setText(taskContent)
            }
        }

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_task_form
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_task_form -> {
                    // Sudah di halaman ini
                    true
                }
                R.id.nav_home -> {
                    if (this !is LoginActivity) {
                        startActivity(Intent(this, LoginActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left) // Form ke Home: slide kanan
                        finish()
                    }
                    true
                }
                R.id.nav_kategori -> {
                    if (this !is KategoriActivity) {
                        startActivity(Intent(this, KategoriActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left) // Form ke Kategori: slide kanan sekali
                        finish()
                    }
                    true
                }
                else -> false
            }
        }

        btnNext.setOnClickListener {
            val title = etTaskTitle.text.toString()
            val content = etTaskContent.text.toString()
            
            if (title.isNotEmpty()) {
                showDateTimePicker { selectedDeadline ->
                    try {
                        val resultIntent = Intent()
                        val fullText = if (content.isNotEmpty()) "$title - $content" else title
                        resultIntent.putExtra("task_text", fullText)
                        resultIntent.putExtra("task_deadline", selectedDeadline)
                        resultIntent.putExtra("task_position", position)
                        resultIntent.putExtra("task_id", editingTaskId)
                        resultIntent.putExtra("is_editing", isEditing)

                        val deadlineDate = dateFormat.parse(selectedDeadline)
                        val deadlineTimestamp = deadlineDate?.time ?: System.currentTimeMillis()
                        
                        // Jika sedang mengedit, batalkan notifikasi lama
                        if (isEditing && editingTaskId != -1) {
                            notificationScheduler.cancelNotifications(editingTaskId)
                        }

                        // Jadwalkan notifikasi baru
                        val taskId = if (editingTaskId != -1) editingTaskId else System.currentTimeMillis().toInt()
                        notificationScheduler.scheduleNotifications(
                            taskId = taskId,
                            taskTitle = title,
                            deadlineTime = deadlineTimestamp,
                            taskStatus = "On Progress"
                        )

                        setResult(RESULT_OK, resultIntent)
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Gagal menyimpan tugas: ${e.message}", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                }
            } else {
                Toast.makeText(this, "Judul tugas tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDateTimePicker(callback: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis

        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            
            // Validasi deadline tidak boleh di masa lalu
            if (calendar.timeInMillis < currentTime) {
                Toast.makeText(this, "Deadline tidak boleh di masa lalu!", Toast.LENGTH_SHORT).show()
                showDateTimePicker(callback) // Tampilkan picker lagi
                return@OnTimeSetListener
            }
            
            callback(dateFormat.format(calendar.time))
        }

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            // Validasi tanggal tidak boleh di masa lalu
            if (calendar.timeInMillis < currentTime) {
                Toast.makeText(this, "Tanggal tidak boleh di masa lalu!", Toast.LENGTH_SHORT).show()
                showDateTimePicker(callback) // Tampilkan picker lagi
                return@OnDateSetListener
            }

            TimePickerDialog(
                this,
                timeSetListener,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        val datePicker = DatePickerDialog(
            this,
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // Set tanggal minimal hari ini
        datePicker.datePicker.minDate = currentTime
        datePicker.show()
    }
} 