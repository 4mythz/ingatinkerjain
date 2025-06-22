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
                "category" to task.category,
                "isDone" to task.isDone,
                "isOverdue" to task.isOverdue,
                "lastModified" to task.lastModified,
                "syncStatus" to task.syncStatus.name,
                "firebaseId" to task.firebaseId,
                "createdAt" to Date(),
                "updatedAt" to Date()
            )

            Log.d(TAG, "Data task yang akan disimpan: $taskMap")

            // Tambahkan ke Firestore dengan document ID yang unik
            val documentId = if (task.firebaseId != null) task.firebaseId!! else "task_${task.id}_${System.currentTimeMillis()}"
            val result = db.collection(COLLECTION_TASKS)
                .document(documentId)
                .set(taskMap)
                .await()

            Log.d(TAG, "Task berhasil ditambahkan ke Firestore dengan document ID: $documentId")
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
                        category = document.getString("category") ?: "Pekerjaan Lainnya",
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
                "category" to task.category,
                "isDone" to task.isDone,
                "isOverdue" to task.isOverdue,
                "lastModified" to task.lastModified,
                "syncStatus" to task.syncStatus.name,
                "firebaseId" to task.firebaseId,
                "updatedAt" to Date()
            )

            // Gunakan document ID yang konsisten
            val documentId = if (task.firebaseId != null) task.firebaseId!! else "task_${task.id}_${System.currentTimeMillis()}"
            db.collection(COLLECTION_TASKS)
                .document(documentId)
                .set(taskMap)
                .await()

            Log.d(TAG, "Task berhasil diupdate di Firestore dengan document ID: $documentId")
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
            
            // Cari document berdasarkan task ID
            val snapshot = db.collection(COLLECTION_TASKS)
                .whereEqualTo("id", taskId)
                .get()
                .await()
            
            if (!snapshot.isEmpty) {
                // Hapus document pertama yang ditemukan
                val document = snapshot.documents.first()
                document.reference.delete().await()
                
                Log.d(TAG, "Task berhasil dihapus dari Firestore dengan document ID: ${document.id}")
                Toast.makeText(context, "Task berhasil dihapus dari cloud", Toast.LENGTH_SHORT).show()
                true
            } else {
                Log.w(TAG, "Task dengan ID $taskId tidak ditemukan di Firestore")
                Toast.makeText(context, "Task tidak ditemukan di cloud", Toast.LENGTH_SHORT).show()
                false
            }

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
            
            // Coba tulis dokumen test untuk memverifikasi write permission
            val testData = hashMapOf(
                "test" to true,
                "timestamp" to System.currentTimeMillis()
            )
            
            val testResult = db.collection("test")
                .document("connection_test")
                .set(testData)
                .await()
            
            Log.d(TAG, "Koneksi ke Firestore berhasil - write permission OK")
            Toast.makeText(context, "Koneksi Firebase berhasil", Toast.LENGTH_SHORT).show()
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error koneksi Firestore: ${e.message}", e)
            Toast.makeText(context, "Error koneksi Firebase: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }
} 