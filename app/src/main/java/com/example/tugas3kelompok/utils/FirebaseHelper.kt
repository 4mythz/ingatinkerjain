package com.example.tugas3kelompok.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.tugas3kelompok.task.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import java.util.*

class FirebaseHelper {
    companion object {
        private const val TAG = "FirebaseHelper"
        private const val COLLECTION_TASKS = "tasks"
    }

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * Menambahkan task baru ke Firestore
     */
    suspend fun addTask(task: Task, context: Context): Boolean {
        return try {
            Log.d(TAG, "Memulai penambahan task ke Firestore: ${task.text}")
            
            // Konversi Task ke Map untuk Firestore
            val taskMap = hashMapOf(
                "id" to task.id,
                "text" to task.text,
                "deadline" to task.deadline,
                "isDone" to task.isDone,
                "isOverdue" to task.isOverdue,
                "lastModified" to task.lastModified,
                "syncStatus" to task.syncStatus.name,
                "firebaseId" to task.firebaseId,
                "createdAt" to Date(),
                "updatedAt" to Date()
            )

            Log.d(TAG, "Data task yang akan disimpan: $taskMap")

            // Tambahkan ke Firestore
            val result = db.collection(COLLECTION_TASKS)
                .document(task.id.toString())
                .set(taskMap)
                .await()

            Log.d(TAG, "Task berhasil ditambahkan ke Firestore dengan ID: ${task.id}")
            Toast.makeText(context, "Task berhasil disinkronkan ke cloud", Toast.LENGTH_SHORT).show()
            true

        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Error Firebase saat menambah task: ${e.message}", e)
            Toast.makeText(context, "Error Firebase: ${e.message}", Toast.LENGTH_LONG).show()
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error umum saat menambah task: ${e.message}", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    /**
     * Mengambil semua task dari Firestore
     */
    suspend fun getAllTasks(): List<Task> {
        return try {
            Log.d(TAG, "Memulai pengambilan semua task dari Firestore")
            
            val snapshot = db.collection(COLLECTION_TASKS)
                .get()
                .await()

            val tasks = mutableListOf<Task>()
            for (document in snapshot.documents) {
                try {
                    val task = Task(
                        id = document.getLong("id")?.toInt() ?: 0,
                        text = document.getString("text") ?: "",
                        deadline = document.getString("deadline") ?: "",
                        isDone = document.getBoolean("isDone") ?: false,
                        isOverdue = document.getBoolean("isOverdue") ?: false,
                        lastModified = document.getLong("lastModified") ?: System.currentTimeMillis(),
                        syncStatus = com.example.tugas3kelompok.task.SyncStatus.valueOf(
                            document.getString("syncStatus") ?: "PENDING"
                        ),
                        firebaseId = document.getString("firebaseId")
                    )
                    tasks.add(task)
                    Log.d(TAG, "Task berhasil diambil: ${task.text}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing task document: ${e.message}")
                }
            }

            Log.d(TAG, "Total task yang diambil: ${tasks.size}")
            tasks

        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Error Firebase saat mengambil tasks: ${e.message}", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error umum saat mengambil tasks: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Update task di Firestore
     */
    suspend fun updateTask(task: Task, context: Context): Boolean {
        return try {
            Log.d(TAG, "Memulai update task di Firestore: ${task.text}")
            
            val taskMap = hashMapOf(
                "id" to task.id,
                "text" to task.text,
                "deadline" to task.deadline,
                "isDone" to task.isDone,
                "isOverdue" to task.isOverdue,
                "lastModified" to task.lastModified,
                "syncStatus" to task.syncStatus.name,
                "firebaseId" to task.firebaseId,
                "updatedAt" to Date()
            )

            db.collection(COLLECTION_TASKS)
                .document(task.id.toString())
                .set(taskMap)
                .await()

            Log.d(TAG, "Task berhasil diupdate di Firestore")
            Toast.makeText(context, "Task berhasil diupdate di cloud", Toast.LENGTH_SHORT).show()
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error saat update task: ${e.message}", e)
            Toast.makeText(context, "Error update: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    /**
     * Hapus task dari Firestore
     */
    suspend fun deleteTask(taskId: Int, context: Context): Boolean {
        return try {
            Log.d(TAG, "Memulai penghapusan task dari Firestore: $taskId")
            
            db.collection(COLLECTION_TASKS)
                .document(taskId.toString())
                .delete()
                .await()

            Log.d(TAG, "Task berhasil dihapus dari Firestore")
            Toast.makeText(context, "Task berhasil dihapus dari cloud", Toast.LENGTH_SHORT).show()
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error saat hapus task: ${e.message}", e)
            Toast.makeText(context, "Error hapus: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    /**
     * Test koneksi ke Firestore
     */
    suspend fun testConnection(context: Context): Boolean {
        return try {
            Log.d(TAG, "Testing koneksi ke Firestore...")
            
            // Coba ambil satu dokumen untuk test koneksi
            db.collection("test")
                .limit(1)
                .get()
                .await()

            Log.d(TAG, "Koneksi ke Firestore berhasil")
            Toast.makeText(context, "Koneksi Firebase berhasil", Toast.LENGTH_SHORT).show()
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error koneksi Firestore: ${e.message}", e)
            Toast.makeText(context, "Error koneksi Firebase: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }
} 