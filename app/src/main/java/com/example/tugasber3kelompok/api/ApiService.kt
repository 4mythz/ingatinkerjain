package com.example.tugasber3kelompok.api

import com.example.tugasber3kelompok.model.NotificationRequest
import com.example.tugasber3kelompok.model.NotificationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("notifications")
    suspend fun sendNotification(@Body request: NotificationRequest): Response<NotificationResponse>
} 