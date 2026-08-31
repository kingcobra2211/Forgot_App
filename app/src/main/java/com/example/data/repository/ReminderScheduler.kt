package com.example.data.repository

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.model.Memory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(memory: Memory, isDaily: Boolean = false) {
        cancel(memory.id)
        var reminderDate = memory.reminderDate ?: return
        if (memory.status != "Active") return

        val isDailySchedule = isDaily || memory.category.equals("Medicine", ignoreCase = true)

        // If daily and time has passed for today, shift to tomorrow
        if (isDailySchedule && reminderDate <= System.currentTimeMillis()) {
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = reminderDate
            }
            val now = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.YEAR, now.get(java.util.Calendar.YEAR))
            calendar.set(java.util.Calendar.MONTH, now.get(java.util.Calendar.MONTH))
            calendar.set(java.util.Calendar.DAY_OF_MONTH, now.get(java.util.Calendar.DAY_OF_MONTH))
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
            reminderDate = calendar.timeInMillis
        }

        if (reminderDate <= System.currentTimeMillis()) return

        try {
            val pendingIntent = reminderPendingIntent(memory, isDailySchedule, reminderDate)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager?.canScheduleExactAlarms() == true) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderDate,
                        pendingIntent
                    )
                } else {
                    alarmManager?.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderDate,
                        pendingIntent
                    )
                }
            } else {
                alarmManager?.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderDate,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancel(memoryId: Int) {
        try {
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                memoryId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun reminderPendingIntent(memory: Memory, isDaily: Boolean = false, scheduledTime: Long = 0L): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(ReminderReceiver.EXTRA_MEMORY_ID, memory.id)
            .putExtra(ReminderReceiver.EXTRA_TITLE, memory.title)
            .putExtra(ReminderReceiver.EXTRA_DESCRIPTION, memory.description)
            .putExtra(ReminderReceiver.EXTRA_CATEGORY, memory.category)
            .putExtra(ReminderReceiver.EXTRA_PHOTO_PATH, memory.photoPath)
            .putExtra(ReminderReceiver.EXTRA_IS_DAILY, isDaily)
            .putExtra(ReminderReceiver.EXTRA_SCHEDULED_TIME, scheduledTime)
        return PendingIntent.getBroadcast(
            context,
            memory.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        createNotificationChannel(context)
        val memoryId = intent.getIntExtra(EXTRA_MEMORY_ID, 0)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Memory reminder" }
        val description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty()
        val category = intent.getStringExtra(EXTRA_CATEGORY).orEmpty()
        val photoPath = intent.getStringExtra(EXTRA_PHOTO_PATH)
        val isDaily = intent.getBooleanExtra(EXTRA_IS_DAILY, false) || category.equals("Medicine", ignoreCase = true)
        val scheduledTime = intent.getLongExtra(EXTRA_SCHEDULED_TIME, System.currentTimeMillis())

        val isMedicine = category.equals("Medicine", ignoreCase = true) || title.contains("Medicine", ignoreCase = true)

        val finalTitle = if (isMedicine) "💊 MEDICINE REMINDER: $title" else title
        val finalDescription = if (isMedicine) {
            "⏰ Time to take your medicine!\n$description".trim()
        } else description

        // Create Launch Activity Intent with target memory ID
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_MEMORY_ID, memoryId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            memoryId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.example.R.drawable.ic_app_logo)
            .setContentTitle(finalTitle)
            .setContentText(finalDescription)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (!photoPath.isNullOrBlank()) {
            val bitmap = try {
                if (photoPath.startsWith("content://")) {
                    val uri = android.net.Uri.parse(photoPath)
                    context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) }
                } else {
                    val file = java.io.File(photoPath)
                    if (file.exists()) android.graphics.BitmapFactory.decodeFile(file.absolutePath) else null
                }
            } catch (e: Exception) {
                null
            }

            if (bitmap != null) {
                notificationBuilder.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .setSummaryText(finalDescription)
                )
                notificationBuilder.setLargeIcon(bitmap)
            } else {
                notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(finalDescription))
            }
        } else {
            notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(finalDescription))
        }

        NotificationManagerCompat.from(context).notify(memoryId.takeIf { it != 0 } ?: System.currentTimeMillis().toInt(), notificationBuilder.build())

        // Auto Reschedule for Tomorrow (+24 Hours) if Daily Repeat
        if (isDaily && memoryId != 0) {
            val nextTime = if (scheduledTime > 0) scheduledTime + 86400000L else System.currentTimeMillis() + 86400000L
            val pendingResult = goAsync()
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val db = com.example.data.database.AppDatabase.getDatabase(context)
                    val memoryWithDetails = db.memoryDao().getMemoryById(memoryId)
                    if (memoryWithDetails != null) {
                        val updatedMemory = memoryWithDetails.memory.copy(
                            reminderDate = nextTime,
                            updatedDate = System.currentTimeMillis()
                        )
                        db.memoryDao().updateMemory(updatedMemory)
                        ReminderScheduler(context).schedule(updatedMemory, isDaily = true)
                    } else {
                        val dummyMemory = Memory(
                            id = memoryId,
                            title = title,
                            description = description,
                            category = category,
                            reminderDate = nextTime,
                            photoPath = photoPath,
                            status = "Active"
                        )
                        ReminderScheduler(context).schedule(dummyMemory, isDaily = true)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Memory reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val EXTRA_MEMORY_ID = "reminder_memory_id"
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_DESCRIPTION = "reminder_description"
        const val EXTRA_CATEGORY = "reminder_category"
        const val EXTRA_PHOTO_PATH = "reminder_photo_path"
        const val EXTRA_IS_DAILY = "reminder_is_daily"
        const val EXTRA_SCHEDULED_TIME = "reminder_scheduled_time"
        private const val CHANNEL_ID = "forgot_memory_reminders"
    }
}
