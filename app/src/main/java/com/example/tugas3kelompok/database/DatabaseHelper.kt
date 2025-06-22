package com.example.tugas3kelompok.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.tugas3kelompok.task.Task
import com.example.tugas3kelompok.task.SyncStatus
import com.example.tugas3kelompok.utils.DeadlineNotificationScheduler

class DatabaseHelper(private val mContext: Context) : SQLiteOpenHelper(mContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "TaskDatabase"
        private const val DATABASE_VERSION = 6
        private const val TABLE_TASKS = "tasks"
        
        // Kolom tabel
        private const val COLUMN_ID = "id"
        private const val COLUMN_TEXT = "text"
        private const val COLUMN_DEADLINE = "deadline"
        private const val COLUMN_CATEGORY = "category"
        private const val COLUMN_IS_DONE = "is_done"
        private const val COLUMN_LAST_MODIFIED = "last_modified"
        private const val COLUMN_SYNC_STATUS = "sync_status"
        private const val COLUMN_FIREBASE_ID = "firebase_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_TASKS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TEXT TEXT NOT NULL,
                $COLUMN_DEADLINE TEXT NOT NULL,
                $COLUMN_CATEGORY TEXT DEFAULT 'Pekerjaan Lainnya',
                $COLUMN_IS_DONE INTEGER DEFAULT 0,
                $COLUMN_LAST_MODIFIED TEXT NOT NULL,
                $COLUMN_SYNC_STATUS TEXT NOT NULL,
                $COLUMN_FIREBASE_ID TEXT
            )
        """.trimIndent()
        
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 5) {
            // Add new columns for sync management
            try {
                db.execSQL("ALTER TABLE $TABLE_TASKS ADD COLUMN $COLUMN_LAST_MODIFIED TEXT DEFAULT '${System.currentTimeMillis()}'")
                db.execSQL("ALTER TABLE $TABLE_TASKS ADD COLUMN $COLUMN_SYNC_STATUS TEXT DEFAULT 'PENDING'")
                db.execSQL("ALTER TABLE $TABLE_TASKS ADD COLUMN $COLUMN_FIREBASE_ID TEXT")
            } catch (e: Exception) {
                // Column might already exist, ignore error
                e.printStackTrace()
            }
        }
        
        if (oldVersion < 6) {
            // Add category column
            try {
                db.execSQL("ALTER TABLE $TABLE_TASKS ADD COLUMN $COLUMN_CATEGORY TEXT DEFAULT 'Pekerjaan Lainnya'")
            } catch (e: Exception) {
                // Column might already exist, ignore error
                e.printStackTrace()
            }
        }
    }

    fun insertTask(task: Task): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TEXT, task.text)
            put(COLUMN_DEADLINE, task.deadline)
            put(COLUMN_CATEGORY, task.category)
            put(COLUMN_IS_DONE, if (task.isDone) 1 else 0)
            put(COLUMN_LAST_MODIFIED, task.lastModified)
            put(COLUMN_SYNC_STATUS, task.syncStatus.name)
            put(COLUMN_FIREBASE_ID, task.firebaseId)
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
            put(COLUMN_CATEGORY, task.category)
            put(COLUMN_IS_DONE, if (task.isDone) 1 else 0)
            put(COLUMN_LAST_MODIFIED, task.lastModified)
            put(COLUMN_SYNC_STATUS, task.syncStatus.name)
            put(COLUMN_FIREBASE_ID, task.firebaseId)
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
                    val category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)) ?: "Pekerjaan Lainnya"
                    val isDone = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DONE)) == 1
                    val lastModified = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_MODIFIED))
                    val syncStatus = SyncStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SYNC_STATUS)))
                    val firebaseId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIREBASE_ID))
                    
                    taskList.add(Task(id, text, deadline, category, isDone, false, lastModified, syncStatus, firebaseId))
                } while (cursor.moveToNext())
            }
        }
        
        return taskList
    }

    fun getTaskById(taskId: Int): Task? {
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_TASKS WHERE $COLUMN_ID = ?"
        
        db.rawQuery(selectQuery, arrayOf(taskId.toString())).use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val text = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT))
                val deadline = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DEADLINE))
                val category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)) ?: "Pekerjaan Lainnya"
                val isDone = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DONE)) == 1
                val lastModified = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_MODIFIED))
                val syncStatus = SyncStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SYNC_STATUS)))
                val firebaseId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIREBASE_ID))
                
                return Task(id, text, deadline, category, isDone, false, lastModified, syncStatus, firebaseId)
            }
        }
        
        return null
    }
} 