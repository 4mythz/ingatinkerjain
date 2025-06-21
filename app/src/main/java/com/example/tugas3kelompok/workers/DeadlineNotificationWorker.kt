package com.example.tugas3kelompok.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.tugas3kelompok.NotificationHelper
import java.text.SimpleDateFormat
import java.util.*

class DeadlineNotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    private val notificationHelper = NotificationHelper(context)

    override fun doWork(): Result {
        val taskId = inputData.getInt("taskId", 0)
        val taskTitle = inputData.getString("taskTitle") ?: return Result.failure()
        val deadlineTime = inputData.getLong("deadlineTime", 0)
        val taskStatus = inputData.getString("taskStatus") ?: "On Progress"
        val timeUntilDeadline = inputData.getString("timeUntilDeadline") ?: ""
        
        val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        val deadlineFormatted = dateFormat.format(Date(deadlineTime))

        val isDeadlineNotification = inputData.getBoolean("isDeadlineNotification", false)
        
        val notificationTitle = taskTitle
        val notificationMessage = if (isDeadlineNotification) {
            "Status: $taskStatus\nDeadline: $deadlineFormatted"
        } else {
            "Status: $taskStatus\nDeadline: $timeUntilDeadline"
        }

        notificationHelper.showTaskNotification(
            notificationTitle,
            notificationMessage,
            taskId
        )

        return Result.success()
    }
} 