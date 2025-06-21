package com.example.tugasber3kelompok.model

data class NotificationResponse(
    val success: Boolean,
    val message: String,
    val notificationId: String? = null
) 