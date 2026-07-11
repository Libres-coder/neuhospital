package com.example.neusoft_hospital.core.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.neusoft_hospital.core.data.local.dao.AppointmentDao
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var appointmentDao: AppointmentDao

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val work = OneTimeWorkRequestBuilder<ReminderWorker>().build()
            WorkManager.getInstance(context).enqueue(work)
        }
    }
}
