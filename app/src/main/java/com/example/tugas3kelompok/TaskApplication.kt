package com.example.tugas3kelompok

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

class TaskApplication : Application() {
    
    companion object {
        private const val TAG = "TaskApplication"
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Configure Firestore for offline persistence
        FirebaseFirestore.getInstance().firestoreSettings = 
            com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        
        Log.d(TAG, "TaskApplication initialized successfully")
    }
} 