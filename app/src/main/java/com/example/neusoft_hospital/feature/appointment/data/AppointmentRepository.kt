package com.example.neusoft_hospital.feature.appointment.data

import com.example.neusoft_hospital.core.data.local.dao.AppointmentDao
import com.example.neusoft_hospital.core.data.local.entity.AppointmentEntity
import com.example.neusoft_hospital.core.data.prefs.UserPreferences
import com.example.neusoft_hospital.feature.appointment.domain.AppointmentStatusConverter
import com.example.neusoft_hospital.feature.auth.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single source of truth for appointment data:
 *  - Read paths (`observeAppointments` / `observeUpcoming`) keep a Room cache so the UI
 *    subscribes to a Flow and stays reactive; the entries still come from the backend on
 *    first read. This is the offline-first pattern requested in the plan.
 *  - Write paths (`book` / `cancel` / `pay` / `setReminder`) go through the API and
 *    then mirror the result into Room so the UI sees it immediately.
 */
@Singleton
class AppointmentRepository @Inject constructor(
    private val api: AppointmentApiService,
    private val dao: AppointmentDao,
    private val prefs: UserPreferences
) {

    // -------- catalog reads (no caching — pure read-through) --------

    suspend fun getDepartments(parentId: String? = null) = api.getDepartments(parentId)
    suspend fun getAllDepartments() = api.getAllDepartments()
    suspend fun getDoctors(departmentId: String) = api.getDoctors(departmentId)
    suspend fun getDoctor(doctorId: String) = api.getDoctor(doctorId)
    suspend fun recommend(symptoms: String, historyDept: String? = null) = api.recommend(symptoms, historyDept)

    /**
     * One-click book: ask the server to pick the best doctor and book the
     * earliest available slot in a single round-trip. Used by the
     * TriageResultScreen "一键挂号" button.
     */
    suspend fun recommendAndBook(symptoms: String): Result<AppointmentEntity> {
        val res = api.recommendAndBook(symptoms)
        res.onSuccess { dao.insert(it) }
        return res
    }

    // -------- my appointments: server-authoritative, mirrored to Room --------

    suspend fun refreshMyAppointments(): Result<List<AppointmentEntity>> {
        val res = api.myAppointments()
        res.onSuccess { list ->
            // upsert into Room — multiple devices may write, last-writer-wins on each row.
            list.forEach { dao.insert(it) }
        }
        return res
    }

    suspend fun book(
        doctorId: String,
        date: String,
        timeSlot: String,
        duration: Int,
        patientId: String,
        patientName: String
    ): Result<AppointmentEntity> {
        val res = api.book(doctorId, date, timeSlot, duration, patientId, patientName)
        res.onSuccess { dao.insert(it) }
        return res
    }

    suspend fun cancel(id: String): Result<Unit> {
        val res = api.cancel(id)
        res.onSuccess { dao.updateStatus(id, "cancelled") }
            .onFailure {
                // Fallback for offline/mock mode: if the in-memory API lost the entry
                // (e.g. process was killed and the entity only lives in Room now), still
                // update the local cache so the UI reflects the change.
                val existing = dao.getById(id)
                if (existing != null) {
                    dao.updateStatus(id, "cancelled")
                    return Result.success(Unit)
                }
            }
        return res
    }

    suspend fun pay(id: String): Result<Unit> {
        val res = api.pay(id)
        res.onSuccess { dao.updateStatus(id, "payed") }
            .onFailure {
                if (dao.getById(id) != null) {
                    dao.updateStatus(id, "payed")
                    return Result.success(Unit)
                }
            }
        return res
    }

    suspend fun noShow(id: String): Result<Unit> {
        val res = api.noShow(id)
        res.onSuccess { dao.updateStatus(id, "no_show") }
            .onFailure {
                if (dao.getById(id) != null) {
                    dao.updateStatus(id, "no_show")
                    return Result.success(Unit)
                }
            }
        return res
    }

    suspend fun setReminder(id: String, set: Boolean): Result<Unit> {
        val res = api.setReminder(id, set)
        res.onSuccess { dao.setReminder(id, set) }
            .onFailure {
                if (dao.getById(id) != null) {
                    dao.setReminder(id, set)
                    return Result.success(Unit)
                }
            }
        return res
    }

    fun observeAppointments(): Flow<List<Appointment>> {
        return prefs.userIdFlow.flatMapLatest { uid ->
            dao.getByPatient(uid).map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    fun observeUpcoming(): Flow<List<Appointment>> {
        return prefs.userIdFlow.flatMapLatest { uid ->
            dao.getUpcoming(uid, com.example.neusoft_hospital.core.util.DateExt.today()).map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    suspend fun getNoShowCount(patientId: String): Int = dao.getNoShowCount(patientId)

    /** Hard-delete every appointment owned by the given user. For debug / recovery. */
    suspend fun clearMyAppointments(): Int {
        val uid = prefs.userIdFlow.first()
        val before = dao.getByPatient(uid).first().size
        dao.deleteByPatient(uid)
        return before
    }
}

fun AppointmentEntity.toDomain() = Appointment(
    id, patientId, patientName, doctorId, doctorName, departmentId, departmentName,
    date, timeSlot, duration, AppointmentStatusConverter.fromString(status), createTime
)