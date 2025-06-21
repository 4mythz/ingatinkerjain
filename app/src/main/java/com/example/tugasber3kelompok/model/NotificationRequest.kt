package com.example.tugasber3kelompok.model

data class NotificationRequest(
    val title: String,
    val message: String,
    val taskId: String,
    val userId: String,
    val scheduledTime: Long
) 