package com.example.tugas3kelompok.data

import android.content.Context
import android.util.Log
import com.example.tugas3kelompok.database.DatabaseHelper
import com.example.tugas3kelompok.task.Task
import com.example.tugas3kelompok.utils.FirebaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HybridDatabaseManager(
    private val context: Context,
    private val databaseHelper: DatabaseHelper,
    private val firebaseHelper: FirebaseHelper
) {
    companion object {
        private const val TAG = "HybridDatabaseManager"
    }

    /**
     * Menambahkan task baru ke SQLite dan Firestore
     */
    fun addTask(task: Task) {
        Log.d(TAG, "Menambahkan task: ${task.text}")
        
        try {
            // Tambahkan ke SQLite terlebih dahulu
            val localId = databaseHelper.insertTask(task)
            if (localId != -1) {
                Log.d(TAG, "Task berhasil ditambahkan ke SQLite dengan ID: $localId")
                
                // Update task dengan ID yang benar
                val updatedTask = task.copy(id = localId)
                
                // Sinkronkan ke Firestore di background
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Log.d(TAG, "Memulai sinkronisasi ke Firestore untuk task ID: $localId")
                        val success = firebaseHelper.addTask(updatedTask, context)
                        if (success) {
                            Log.d(TAG, "Task berhasil disinkronkan ke Firestore")
                        } else {
                            Log.e(TAG, "Gagal sinkronkan task ke Firestore")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saat sinkronisasi ke Firestore: ${e.message}", e)
                    }
                }
            } else {
                Log.e(TAG, "Gagal menambahkan task ke SQLite")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dalam addTask: ${e.message}", e)
        }
    }

    /**
     * Mengambil semua task dari SQLite (primary source)
     */
    fun getAllTasks(): List<Task> {
        Log.d(TAG, "Mengambil semua task dari SQLite")
        return databaseHelper.getAllTasks()
    }

    /**
     * Update task di SQLite dan Firestore
     */
    fun updateTask(task: Task) {
        Log.d(TAG, "Update task: ${task.text}")
        
        // Update di SQLite terlebih dahulu
        val success = databaseHelper.updateTask(task)
        if (success > 0) {
            Log.d(TAG, "Task berhasil diupdate di SQLite")
            
            // Sinkronkan ke Firestore di background
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val firebaseSuccess = firebaseHelper.updateTask(task, context)
                    if (firebaseSuccess) {
                        Log.d(TAG, "Task berhasil diupdate di Firestore")
                    } else {
                        Log.e(TAG, "Gagal update task di Firestore")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error saat update di Firestore: ${e.message}", e)
                }
            }
        } else {
            Log.e(TAG, "Gagal update task di SQLite")
        }
    }

    /**
     * Hapus task dari SQLite dan Firestore
     */
    fun deleteTask(taskId: Int) {
        Log.d(TAG, "Hapus task dengan ID: $taskId")
        
        // Ambil task untuk mendapatkan data lengkap
        val task = databaseHelper.getTaskById(taskId)
        if (task != null) {
            // Hapus dari SQLite terlebih dahulu
            val success = databaseHelper.deleteTask(task)
            if (success > 0) {
                Log.d(TAG, "Task berhasil dihapus dari SQLite")
                
                // Hapus dari Firestore di background
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val firebaseSuccess = firebaseHelper.deleteTask(taskId, context)
                        if (firebaseSuccess) {
                            Log.d(TAG, "Task berhasil dihapus dari Firestore")
                        } else {
                            Log.e(TAG, "Gagal hapus task dari Firestore")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saat hapus dari Firestore: ${e.message}", e)
                    }
                }
            } else {
                Log.e(TAG, "Gagal hapus task dari SQLite")
            }
        } else {
            Log.e(TAG, "Task tidak ditemukan dengan ID: $taskId")
        }
    }

    /**
     * Sinkronisasi manual dari Firestore ke SQLite
     */
    suspend fun syncFromFirestore() {
        Log.d(TAG, "Memulai sinkronisasi dari Firestore")
        
        try {
            val firestoreTasks = firebaseHelper.getAllTasks()
            Log.d(TAG, "Mengambil ${firestoreTasks.size} task dari Firestore")
            
            for (firestoreTask in firestoreTasks) {
                // Cek apakah task sudah ada di SQLite
                val existingTask = databaseHelper.getTaskById(firestoreTask.id)
                
                if (existingTask == null) {
                    // Task belum ada, tambahkan ke SQLite
                    Log.d(TAG, "Menambahkan task baru dari Firestore: ${firestoreTask.text}")
                    databaseHelper.insertTask(firestoreTask)
                } else {
                    // Task sudah ada, update jika perlu
                    Log.d(TAG, "Task sudah ada di SQLite: ${firestoreTask.text}")
                }
            }
            
            Log.d(TAG, "Sinkronisasi dari Firestore selesai")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saat sinkronisasi dari Firestore: ${e.message}", e)
        }
    }

    /**
     * Test koneksi Firebase
     */
    suspend fun testFirebaseConnection(): Boolean {
        return try {
            Log.d(TAG, "Testing koneksi Firebase...")
            firebaseHelper.testConnection(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error test koneksi Firebase: ${e.message}", e)
            false
        }
    }
} 