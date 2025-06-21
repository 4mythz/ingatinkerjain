package com.example.tugas3kelompok.utils

import android.content.Context
import androidx.work.*
import com.example.tugas3kelompok.workers.DeadlineNotificationWorker
import java.util.concurrent.TimeUnit

class DeadlineNotificationScheduler(private val context: Context) {

    fun scheduleNotifications(
        taskId: Int,
        taskTitle: String,
        deadlineTime: Long,
        taskStatus: String
    ) {
        try {
            // API call dinonaktifkan untuk sementara
            // sendNotificationToApi(taskId, taskTitle, deadlineTime, taskStatus)
            
            cancelNotifications(taskId)

            val currentTime = System.currentTimeMillis()
            val timeUntilDeadline = deadlineTime - currentTime

            if (timeUntilDeadline <= 0) return

            val threeDays = TimeUnit.DAYS.toMillis(3)
            val oneDay = TimeUnit.DAYS.toMillis(1)
            val sixHours = TimeUnit.HOURS.toMillis(6)
            val oneHour = TimeUnit.HOURS.toMillis(1)

            val notificationTimes = mapOf(
                threeDays to "3 hari sebelum deadline",
                oneDay to "24 jam sebelum deadline",
                sixHours to "6 jam sebelum deadline",
                oneHour to "1 jam sebelum deadline"
            )

            notificationTimes.forEach { (interval, message) ->
                if (timeUntilDeadline > interval) {
                    val delayTime = timeUntilDeadline - interval
                    if (delayTime > 0) {
                        scheduleNotification(
                            taskId,
                            taskTitle,
                            deadlineTime,
                            taskStatus,
                            delayTime,
                            message,
                            false
                        )
                    }
                }
            }

            if (timeUntilDeadline > 0) {
                scheduleNotification(
                    taskId,
                    taskTitle,
                    deadlineTime,
                    "Terlewat",
                    timeUntilDeadline,
                    "",
                    true
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationScheduler", "Error scheduling notifications: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun scheduleNotification(
        taskId: Int,
        taskTitle: String,
        deadlineTime: Long,
        taskStatus: String,
        delay: Long,
        timeUntilDeadline: String,
        isDeadlineNotification: Boolean
    ) {
        try {
            val workManager = WorkManager.getInstance(context)

            workManager.cancelAllWorkByTag("notification_${taskId}_${timeUntilDeadline}")

            val inputData = workDataOf(
                "taskId" to taskId,
                "taskTitle" to taskTitle,
                "deadlineTime" to deadlineTime,
                "taskStatus" to taskStatus,
                "timeUntilDeadline" to timeUntilDeadline,
                "isDeadlineNotification" to isDeadlineNotification
            )

            val notificationWork = OneTimeWorkRequestBuilder<DeadlineNotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("task_${taskId}")
                .addTag("notification_${taskId}_${timeUntilDeadline}")
                .build()

            val uniqueWorkName = "notification_${taskId}_${timeUntilDeadline}_${System.currentTimeMillis()}"
            
            workManager.enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.REPLACE,
                notificationWork
            )
        } catch (e: Exception) {
            android.util.Log.e("NotificationScheduler", "Error scheduling notification: ${e.message}")
            e.printStackTrace()
        }
    }

    fun cancelNotifications(taskId: Int) {
        try {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag("task_${taskId}")
        } catch (e: Exception) {
            android.util.Log.e("NotificationScheduler", "Error canceling notifications: ${e.message}")
            e.printStackTrace()
        }
    }
} 