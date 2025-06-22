package com.example.tugas3kelompok

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tugas3kelompok.database.DatabaseHelper
import com.example.tugas3kelompok.task.Task
import com.example.tugas3kelompok.utils.DeadlineNotificationScheduler
import com.example.tugas3kelompok.data.HybridDatabaseManager
import com.example.tugas3kelompok.utils.FirebaseHelper
import java.text.SimpleDateFormat
import java.util.*

class TaskFormActivity : AppCompatActivity() {
    private lateinit var etTaskTitle: EditText
    private lateinit var etTaskContent: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnNext: Button
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private lateinit var notificationScheduler: DeadlineNotificationScheduler
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var hybridDatabaseManager: HybridDatabaseManager
    private var editingTaskId: Int = -1
    private var isEditing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_form)

        try {
            // Initialize database and notification scheduler
            dbHelper = DatabaseHelper(this)
            firebaseHelper = FirebaseHelper()
            hybridDatabaseManager = HybridDatabaseManager(this, dbHelper, firebaseHelper)
            notificationScheduler = DeadlineNotificationScheduler(this)

            etTaskTitle = findViewById(R.id.etTaskTitle)
            etTaskContent = findViewById(R.id.etTaskContent)
            spinnerCategory = findViewById(R.id.spinnerCategory)
            btnNext = findViewById(R.id.btnNext)

            // Setup category spinner
            setupCategorySpinner()

            // Get task data if editing
            editingTaskId = intent.getIntExtra("task_id", -1)
            isEditing = intent.getBooleanExtra("is_editing", false)
            val taskText = intent.getStringExtra("task_text")
            val taskContent = intent.getStringExtra("task_content") ?: ""
            val taskDeadline = intent.getStringExtra("task_deadline")
            val taskCategory = intent.getStringExtra("task_category") ?: "Pekerjaan Lainnya"
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

            // Set category if editing or if category is passed from category page
            if (isEditing || taskCategory != "Pekerjaan Lainnya") {
                val categoryPosition = when (taskCategory) {
                    "Pekerjaan Rumah" -> 0
                    "Pekerjaan Sekolah" -> 1
                    "Pekerjaan Kantor" -> 2
                    "Pekerjaan Lainnya" -> 3
                    else -> 3
                }
                spinnerCategory.setSelection(categoryPosition)
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
                        if (this !is HomeActivity) {
                            startActivity(Intent(this, HomeActivity::class.java))
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
                val category = spinnerCategory.selectedItem.toString()
                
                if (title.isNotEmpty()) {
                    showDateTimePicker { selectedDeadline ->
                        try {
                            val fullText = if (content.isNotEmpty()) "$title - $content" else title
                            
                            if (isEditing && editingTaskId != -1) {
                                // Update existing task using HybridDatabaseManager
                                val existingTask = dbHelper.getTaskById(editingTaskId)
                                if (existingTask != null) {
                                    val updatedTask = existingTask.copy(
                                        text = fullText,
                                        deadline = selectedDeadline,
                                        category = category
                                    )
                                    hybridDatabaseManager.updateTask(updatedTask)
                                    
                                    // Reschedule notifications
                                    val deadlineDate = dateFormat.parse(selectedDeadline)
                                    val deadlineTimestamp = deadlineDate?.time ?: System.currentTimeMillis()
                                    notificationScheduler.cancelNotifications(editingTaskId)
                                    notificationScheduler.scheduleNotifications(
                                        taskId = editingTaskId,
                                        taskTitle = title,
                                        deadlineTime = deadlineTimestamp,
                                        taskStatus = "On Progress"
                                    )
                                    
                                    Toast.makeText(this, "Tugas berhasil diperbarui", Toast.LENGTH_SHORT).show()
                                    
                                    // Send result back to MainActivity
                                    val resultIntent = Intent().apply {
                                        putExtra("task_text", fullText)
                                        putExtra("task_deadline", selectedDeadline)
                                        putExtra("task_category", category)
                                        putExtra("task_position", intent.getIntExtra("task_position", -1))
                                        putExtra("task_id", editingTaskId)
                                    }
                                    setResult(RESULT_OK, resultIntent)
                                    finish()
                                }
                            } else {
                                // Create new task using HybridDatabaseManager
                                val task = Task(text = fullText, deadline = selectedDeadline, category = category)
                                hybridDatabaseManager.addTask(task)
                                
                                // Schedule notifications
                                val deadlineDate = dateFormat.parse(selectedDeadline)
                                val deadlineTimestamp = deadlineDate?.time ?: System.currentTimeMillis()
                                notificationScheduler.scheduleNotifications(
                                    taskId = task.id,
                                    taskTitle = title,
                                    deadlineTime = deadlineTimestamp,
                                    taskStatus = "On Progress"
                                )
                                
                                Toast.makeText(this, "Tugas berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                                // Navigasi ke halaman kategori yang sesuai
                                val categoryIntent = when (category) {
                                    "Pekerjaan Rumah" -> Intent(this, RumahActivity::class.java)
                                    "Pekerjaan Sekolah" -> Intent(this, SekolahActivity::class.java)
                                    "Pekerjaan Kantor" -> Intent(this, KantorActivity::class.java)
                                    "Pekerjaan Lainnya" -> Intent(this, LainnyaActivity::class.java)
                                    else -> Intent(this, MainActivity::class.java)
                                }
                                categoryIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(categoryIntent)
                                finish() // Tutup form setelah selesai
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "Gagal menyimpan tugas: ${e.message}", Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                    }
                } else {
                    Toast.makeText(this, "Judul tugas tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memuat form: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            finish()
        }
    }

    private fun setupCategorySpinner() {
        val categories = arrayOf(
            "Pekerjaan Rumah",
            "Pekerjaan Sekolah", 
            "Pekerjaan Kantor",
            "Pekerjaan Lainnya"
        )
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
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

    override fun onDestroy() {
        try {
            super.onDestroy()
            dbHelper.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 