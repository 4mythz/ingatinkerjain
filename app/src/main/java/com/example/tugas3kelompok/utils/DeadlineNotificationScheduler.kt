package com.example.tugas3kelompok.utils

import android.content.Context
import androidx.work.*
import com.example.tugas3kelompok.workers.DeadlineNotificationWorker
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Callback
import retrofit2.Response

// Data class untuk request
data class NotifRequest(
    val taskId: Int,
    val title: String,
    val deadline: Long,
    val status: String
)

// Interface API
interface NotifApiService {
    @POST("api/notify")
    fun sendNotif(@Body request: NotifRequest): Call<Void>
}

class DeadlineNotificationScheduler(private val context: Context) {

    fun scheduleNotifications(
        taskId: Int,
        taskTitle: String,
        deadlineTime: Long,
        taskStatus: String
    ) {
        // Kirim data ke API
        val retrofit = Retrofit.Builder()
            .baseUrl("https://your-api-url.com/") // Ganti dengan URL API kamu
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(NotifApiService::class.java)
        val notifRequest = NotifRequest(
            taskId = taskId,
            title = taskTitle,
            deadline = deadlineTime,
            status = taskStatus
        )
        api.sendNotif(notifRequest).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                // Sukses kirim ke server
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Gagal kirim ke server
            }
        })

        // Batalkan notifikasi lama terlebih dahulu
        cancelNotifications(taskId)

        val currentTime = System.currentTimeMillis()
        val timeUntilDeadline = deadlineTime - currentTime

        // Jika deadline sudah lewat, jangan jadwalkan notifikasi
        if (timeUntilDeadline <= 0) return

        // Define notification intervals (in milliseconds)
        val threeDays = TimeUnit.DAYS.toMillis(3)
        val oneDay = TimeUnit.DAYS.toMillis(1)
        val sixHours = TimeUnit.HOURS.toMillis(6)
        val oneHour = TimeUnit.HOURS.toMillis(1)

        // Schedule notifications before deadline
        val notificationTimes = mapOf(
            threeDays to "3 hari sebelum deadline",
            oneDay to "24 jam sebelum deadline",
            sixHours to "6 jam sebelum deadline",
            oneHour to "1 jam sebelum deadline"
        )

        // Jadwalkan notifikasi sesuai interval yang tersisa
        notificationTimes.forEach { (interval, message) ->
            if (timeUntilDeadline > interval) {
                val delayTime = timeUntilDeadline - interval
                if (delayTime > 0) {  // Pastikan delay tidak negatif
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

        // Schedule notification at deadline
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

            // Batalkan work yang ada dengan tag yang sama
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

            // Gunakan unique work name yang berbeda untuk setiap notifikasi
            val uniqueWorkName = "notification_${taskId}_${timeUntilDeadline}_${System.currentTimeMillis()}"
            
            workManager.enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.REPLACE,
                notificationWork
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelNotifications(taskId: Int) {
        try {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag("task_${taskId}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 