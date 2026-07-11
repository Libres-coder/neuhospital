package com.example.neusoft_hospital.feature.appointment.domain

import com.example.neusoft_hospital.feature.auth.domain.AppointmentStatus

object AppointmentStatusConverter {
    fun fromString(s: String): AppointmentStatus = runCatching {
        AppointmentStatus.valueOf(s.uppercase())
    }.getOrDefault(AppointmentStatus.PENDING)

    fun toString(s: AppointmentStatus): String = s.name.lowercase()
}