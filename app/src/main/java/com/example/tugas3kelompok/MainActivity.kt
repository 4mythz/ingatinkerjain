package com.example.tugas3kelompok

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.tugas3kelompok.database.DatabaseHelper
import com.example.tugas3kelompok.task.Task
import com.example.tugas3kelompok.task.TaskAdapter
import com.example.tugas3kelompok.utils.DeadlineNotificationScheduler
import com.example.tugas3kelompok.data.HybridDatabaseManager
import com.example.tugas3kelompok.utils.FirebaseHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var btnAddTask: Button
    private lateinit var taskListView: ListView
    private lateinit var notificationScheduler: DeadlineNotificationScheduler
    private val taskList = mutableListOf<Task>()
    private lateinit var adapter: TaskAdapter
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var hybridDatabaseManager: HybridDatabaseManager
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val handler = Handler(Looper.getMainLooper())

    // Request notification permission
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notifikasi tidak akan muncul tanpa izin", Toast.LENGTH_LONG).show()
        }
    }

    private val addTaskLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            try {
                val data = result.data
                val taskText = data?.getStringExtra("task_text") ?: return@registerForActivityResult
                val taskDeadline = data.getStringExtra("task_deadline") ?: return@registerForActivityResult
                val task = Task(text = taskText, deadline = taskDeadline)
                
                // Use hybrid database manager for sync
                hybridDatabaseManager.addTask(task)
                loadTasks()
                
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal menambah tugas: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private val editTaskLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            try {
                val data = result.data
                val taskText = data?.getStringExtra("task_text") ?: return@registerForActivityResult
                val taskDeadline = data.getStringExtra("task_deadline") ?: return@registerForActivityResult
                val position = data.getIntExtra("task_position", -1)
                val taskId = data.getIntExtra("task_id", -1)
                
                if (position != -1 && position < taskList.size) {
                    val existingTask = taskList[position]
                    val updatedTask = existingTask.copy(
                        id = existingTask.id,
                        text = taskText,
                        deadline = taskDeadline,
                        isDone = existingTask.isDone
                    )

                    // Update menggunakan hybrid manager
                    hybridDatabaseManager.updateTask(updatedTask)

                    // Jadwalkan ulang notifikasi
                    try {
                        val deadlineDate = dateFormat.parse(taskDeadline)
                        val deadlineTimestamp = deadlineDate?.time ?: System.currentTimeMillis()
                        
                        // Batalkan notifikasi lama dan jadwalkan yang baru
                        notificationScheduler.cancelNotifications(updatedTask.id)
                        notificationScheduler.scheduleNotifications(
                            taskId = updatedTask.id,
                            taskTitle = taskText.split(" - ")[0], // Ambil judul saja
                            deadlineTime = deadlineTimestamp,
                            taskStatus = "On Progress"
                        )
                    } catch (e: Exception) {
                        Toast.makeText(this, "Gagal menjadwalkan notifikasi: ${e.message}", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }

                    loadTasks()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal mengubah tugas: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request notification permission for Android 13 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        try {
            // Initialize notification scheduler
            notificationScheduler = DeadlineNotificationScheduler(this)
            
            // Initialize views
            btnAddTask = findViewById(R.id.btnAddTask)
            taskListView = findViewById(R.id.taskListView)
            
            // Initialize database
            dbHelper = DatabaseHelper(this)
            
            // Initialize Firebase helper
            firebaseHelper = FirebaseHelper()

            // Initialize hybrid database manager
            hybridDatabaseManager = HybridDatabaseManager(this, dbHelper, firebaseHelper)

            // Initialize adapter
            adapter = TaskAdapter(
                this,
                taskList,
                onTaskChecked = { task ->
                    try {
                        task.isDone = true
                        hybridDatabaseManager.updateTask(task)
                        notificationScheduler.cancelNotifications(task.id)
                        loadTasks()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Gagal mengubah status tugas: ${e.message}", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                },
                onTaskEdit = { task ->
                    try {
                        val position = taskList.indexOf(task)
                        if (position != -1) {
                            val intent = Intent(this, TaskFormActivity::class.java).apply {
                                putExtra("task_text", task.text)
                                putExtra("task_content", "")
                                putExtra("task_deadline", task.deadline)
                                putExtra("task_position", position)
                                putExtra("task_id", task.id)
                                putExtra("is_editing", true)
                            }
                            editTaskLauncher.launch(intent)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Gagal memulai edit tugas: ${e.message}", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                },
                onTaskDelete = { task ->
                    try {
                        notificationScheduler.cancelNotifications(task.id)
                        hybridDatabaseManager.deleteTask(task.id)
                        loadTasks()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Gagal menghapus tugas: ${e.message}", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                }
            )

            // Set adapter to ListView
            taskListView.adapter = adapter

            // Load tasks
            loadTasks()

            btnAddTask.setOnClickListener {
                val intent = Intent(this, TaskFormActivity::class.java)
                addTaskLauncher.launch(intent)
            }

            startAutoCompleteChecker()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memuat aplikasi: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            finish()
        }
    }

    private fun loadTasks() {
        try {
            taskList.clear()
            taskList.addAll(hybridDatabaseManager.getAllTasks())
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memuat daftar tugas: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun startAutoCompleteChecker() {
        val runnable = object : Runnable {
            override fun run() {
                try {
                    val now = Date()
                    var updated = false

                    for (task in taskList) {
                        if (!task.isDone) {
                            try {
                                val deadlineDate = dateFormat.parse(task.deadline)
                                if (deadlineDate != null && deadlineDate.before(now)) {
                                    // Task is overdue, update UI
                                    task.isOverdue = true
                                    updated = true
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    if (updated) {
                        runOnUiThread {
                            adapter.notifyDataSetChanged()
                        }
                    }

                    // Check every 1 minute
                    handler.postDelayed(this, 60000)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        handler.post(runnable)
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
            handler.removeCallbacksAndMessages(null)
            dbHelper.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBackPressed() {
        try {
            super.onBackPressed()
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal kembali ke halaman login: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
