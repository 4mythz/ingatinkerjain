package com.example.tugas3kelompok

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class TaskManagerApplication : Application() {
    
    companion object {
        private const val TAG = "TaskManagerApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "Firebase berhasil diinisialisasi")
        } catch (e: Exception) {
            Log.e(TAG, "Error saat inisialisasi Firebase: ${e.message}", e)
        }
    }
} 