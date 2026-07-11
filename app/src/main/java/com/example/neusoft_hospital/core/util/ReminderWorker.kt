package com.example.neusoft_hospital.core.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.neusoft_hospital.core.data.local.dao.AppointmentDao
import com.example.neusoft_hospital.core.data.local.entity.AppointmentEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val appointmentDao: AppointmentDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        createNotificationChannel()
        val today = DateExt.today()
        val upcoming: List<AppointmentEntity> = appointmentDao.getUpcoming(today).first()
        upcoming.forEach { appt ->
            val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("预约提醒")
                .setContentText("您有预约：${appt.doctorName} 医生 ${appt.date} ${appt.timeSlot}")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(applicationContext).notify(appt.id.hashCode(), notif)
        }
        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "预约提醒", NotificationManager.IMPORTANCE_HIGH)
            applicationContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object { const val CHANNEL_ID = "hospital_reminder" }
}