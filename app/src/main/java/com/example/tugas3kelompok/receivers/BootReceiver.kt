package com.example.tugas3kelompok.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tugas3kelompok.database.DatabaseHelper
import com.example.tugas3kelompok.utils.DeadlineNotificationScheduler
import java.text.SimpleDateFormat
import java.util.*

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val dbHelper = DatabaseHelper(context)
            val notificationScheduler = DeadlineNotificationScheduler(context)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            
            // Reschedule notifications for all non-completed tasks
            dbHelper.getAllTasks().forEach { task ->
                if (!task.isDone) {
                    try {
                        val deadlineDate = dateFormat.parse(task.deadline)
                        val deadlineTimestamp = deadlineDate?.time ?: return@forEach
                        
                        // Only schedule if deadline hasn't passed
                        if (deadlineTimestamp > System.currentTimeMillis()) {
                            notificationScheduler.scheduleNotifications(
                                taskId = task.id,
                                taskTitle = task.text,
                                deadlineTime = deadlineTimestamp,
                                taskStatus = "On Progress"
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            dbHelper.close()
        }
    }
} 