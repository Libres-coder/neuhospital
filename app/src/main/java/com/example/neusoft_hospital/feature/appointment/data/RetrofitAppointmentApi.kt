package com.example.neusoft_hospital.feature.appointment.data

import com.example.neusoft_hospital.core.data.local.entity.AppointmentEntity
import com.example.neusoft_hospital.feature.auth.domain.*
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-network adapter for the appointment module. Mirrors [MockAppointmentApi].
 *
 * Naming conflicts: the entity class [AppointmentEntity] (Room) and the
 * domain class [Appointment] are both used here, so they are imported
 * individually below.
 */
@Singleton
class RetrofitAppointmentApi @Inject constructor(
    private val service: AppointmentApiServiceRetrofit
) : AppointmentApiService {

    override suspend fun getDepartments(parentId: String?): Result<List<Department>> = wrap {
        val resp = service.listDepartments(parentId)
        requireOk(resp.code, resp.message)
        resp.data.orEmpty().map { it.toDomain() }
    }

    override suspend fun getAllDepartments(): Result<List<Department>> = wrap {
        val resp = service.listAllDepartments()
        requireOk(resp.code, resp.message)
        resp.data.orEmpty().map { it.toDomain() }
    }

    override suspend fun getDoctors(departmentId: String): Result<List<Doctor>> = wrap {
        val resp = service.listDoctors(departmentId)
        requireOk(resp.code, resp.message)
        resp.data.orEmpty().map { it.toDomain() }
    }

    override suspend fun getDoctor(doctorId: String): Result<Doctor> = wrap {
        val resp = service.getDoctor(doctorId)
        requireOk(resp.code, resp.message)
        resp.data?.toDomain() ?: error("empty doctor")
    }

    override suspend fun recommend(symptoms: String, historyDept: String?): Result<List<DoctorRecommendation>> = wrap {
        val resp = service.recommend(symptoms, historyDept)
        requireOk(resp.code, resp.message)
        resp.data.orEmpty().map { rec ->
            DoctorRecommendation(rec.doctor.toDomain(), rec.score, emptyList())
        }
    }

    override suspend fun book(
        doctorId: String,
        date: String,
        timeSlot: String,
        duration: Int,
        patientId: String,
        patientName: String
    ): Result<AppointmentEntity> = wrap {
        val resp = service.book(BookReqDto(doctorId, date, timeSlot, duration, patientId, patientName))
        requireOk(resp.code, resp.message)
        resp.data?.toEntity() ?: error("empty appointment")
    }

    override suspend fun cancel(id: String): Result<Unit> = wrap {
        val resp = service.cancel(id)
        requireOk(resp.code, resp.message)
    }

    override suspend fun pay(id: String): Result<Unit> = wrap {
        val resp = service.pay(id)
        requireOk(resp.code, resp.message)
    }

    override suspend fun noShow(id: String): Result<Unit> = wrap {
        val resp = service.noShow(id)
        requireOk(resp.code, resp.message)
    }

    override suspend fun setReminder(id: String, set: Boolean): Result<Unit> = wrap {
        val resp = service.setReminder(id, set)
        requireOk(resp.code, resp.message)
    }

    override suspend fun myAppointments(): Result<List<AppointmentEntity>> = wrap {
        val resp = service.mine()
        requireOk(resp.code, resp.message)
        resp.data.orEmpty().map { it.toEntity() }
    }

    override suspend fun noShowCount(): Result<Int> = wrap {
        val resp = service.noShowCount()
        requireOk(resp.code, resp.message)
        resp.data ?: 0
    }

    // ---------------- mappers ----------------

    private fun DepartmentDto.toDomain() = Department(
        id = id, parentId = parentId, name = name,
        namePy = namePy ?: "", desc = desc ?: "",
        iconUrl = iconUrl
    )

    private fun DoctorDto.toDomain() = Doctor(
        id = id, name = name,
        departmentId = departmentId, departmentName = departmentName,
        title = title, expertise = expertise ?: "",
        profile = profile ?: "", rating = rating,
        avatarUrl = avatarUrl,
        schedule = schedule.orEmpty().map { s ->
            Schedule(
                date = s.date, dayOfWeek = s.dayOfWeek,
                slots = s.slots.map { t ->
                    TimeSlot(t.id, t.startTime, t.endTime, t.available, t.total)
                }
            )
        }
    )

    private fun AppointmentDto.toEntity() = AppointmentEntity(
        id = id, patientId = patientId, patientName = patientName,
        doctorId = doctorId, doctorName = doctorName,
        departmentId = departmentId, departmentName = departmentName,
        date = date, timeSlot = timeSlot, duration = duration,
        status = status, createTime = createTime,
        reminderSet = reminderSet
    )

    // ---------------- helpers ----------------

    private fun requireOk(code: Int, message: String?) {
        if (code != 0) throw RuntimeException(message ?: "api failed (code=$code)")
    }

    private inline fun <T> wrap(block: () -> T): Result<T> = runCatching { block() }.mapHttpError()

    private fun <T> Result<T>.mapHttpError(): Result<T> = recoverCatching { t ->
        throw when (t) {
            is HttpException -> RuntimeException("HTTP ${t.code()}: ${t.message()}")
            else -> t
        }
    }
}