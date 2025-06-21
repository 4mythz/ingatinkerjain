package com.example.tugas3kelompok.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.tugas3kelompok.task.Task
import com.example.tugas3kelompok.utils.DeadlineNotificationScheduler

class DatabaseHelper(private val mContext: Context) : SQLiteOpenHelper(mContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "TaskDatabase"
        private const val DATABASE_VERSION = 4
        private const val TABLE_TASKS = "tasks"
        
        // Kolom tabel
        private const val COLUMN_ID = "id"
        private const val COLUMN_TEXT = "text"
        private const val COLUMN_DEADLINE = "deadline"
        private const val COLUMN_IS_DONE = "is_done"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_TASKS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TEXT TEXT NOT NULL,
                $COLUMN_DEADLINE TEXT NOT NULL,
                $COLUMN_IS_DONE INTEGER DEFAULT 0
            )
        """.trimIndent()
        
        db.execSQL(createTable)
    }
//tes
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
        onCreate(db)
    }

    fun insertTask(task: Task): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TEXT, task.text)
            put(COLUMN_DEADLINE, task.deadline)
            put(COLUMN_IS_DONE, if (task.isDone) 1 else 0)
        }
        val id = db.insert(TABLE_TASKS, null, values).toInt()
        task.id = id
        return id
    }

    fun updateTask(task: Task): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TEXT, task.text)
            put(COLUMN_DEADLINE, task.deadline)
            put(COLUMN_IS_DONE, if (task.isDone) 1 else 0)
        }
        
        // Cancel old notifications before update
        try {
            val notificationScheduler = DeadlineNotificationScheduler(mContext)
            notificationScheduler.cancelNotifications(task.id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return db.update(TABLE_TASKS, values, "$COLUMN_ID = ?", arrayOf(task.id.toString()))
    }

    fun deleteTask(task: Task): Int {
        val db = this.writableDatabase
        
        // Cancel notifications when deleting task
        try {
            val notificationScheduler = DeadlineNotificationScheduler(mContext)
            notificationScheduler.cancelNotifications(task.id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return db.delete(TABLE_TASKS, "$COLUMN_ID = ?", arrayOf(task.id.toString()))
    }

    fun getAllTasks(): List<Task> {
        val taskList = mutableListOf<Task>()
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_TASKS"
        
        db.rawQuery(selectQuery, null).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                    val text = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT))
                    val deadline = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DEADLINE))
                    val isDone = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DONE)) == 1
                    
                    taskList.add(Task(id, text, deadline, isDone))
                } while (cursor.moveToNext())
            }
        }
        
        return taskList
    }
} 