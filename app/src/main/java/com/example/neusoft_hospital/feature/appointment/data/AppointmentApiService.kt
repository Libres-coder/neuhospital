package com.example.neusoft_hospital.feature.appointment.data

import com.example.neusoft_hospital.core.data.local.entity.AppointmentEntity
import com.example.neusoft_hospital.core.network.ApiEnvelope
import com.example.neusoft_hospital.feature.auth.domain.*
import retrofit2.http.*

/**
 * Abstraction over the appointment backend.
 * Same shape as [AuthApiService]: implementations are
 *  - [MockAppointmentApi] (offline)
 *  - [RetrofitAppointmentApi] (real backend)
 *
 * All methods return [Result] so the data layer doesn't care which one is wired.
 */
interface AppointmentApiService {

    suspend fun getDepartments(parentId: String? = null): Result<List<Department>>
    suspend fun getAllDepartments(): Result<List<Department>>
    suspend fun getDoctors(departmentId: String): Result<List<Doctor>>
    suspend fun getDoctor(doctorId: String): Result<Doctor>
    suspend fun recommend(symptoms: String, historyDept: String? = null): Result<List<DoctorRecommendation>>

    suspend fun recommendAndBook(symptoms: String): Result<AppointmentEntity>

    suspend fun book(
        doctorId: String,
        date: String,
        timeSlot: String,
        duration: Int,
        patientId: String,
        patientName: String
    ): Result<AppointmentEntity>

    suspend fun cancel(id: String): Result<Unit>
    suspend fun pay(id: String): Result<Unit>
    suspend fun noShow(id: String): Result<Unit>
    suspend fun setReminder(id: String, set: Boolean): Result<Unit>

    suspend fun myAppointments(): Result<List<AppointmentEntity>>
    suspend fun noShowCount(): Result<Int>
}

// -------------------- Retrofit DTOs --------------------

data class DepartmentDto(
    val id: String,
    val parentId: String?,
    val name: String,
    val namePy: String?,
    val desc: String? = null,
    val iconUrl: String? = null
)

data class DoctorDto(
    val id: String,
    val name: String,
    val departmentId: String,
    val departmentName: String,
    val title: String,
    val expertise: String?,
    val profile: String?,
    val rating: Float,
    val avatarUrl: String? = null,
    val schedule: List<ScheduleDto>? = null
)

data class ScheduleDto(
    val date: String,
    val dayOfWeek: String,
    val slots: List<TimeSlotDto>
)

data class TimeSlotDto(
    val id: String,
    val startTime: String,
    val endTime: String,
    val available: Int,
    val total: Int
)

data class AppointmentDto(
    val id: String,
    val patientId: String,
    val patientName: String,
    val doctorId: String,
    val doctorName: String,
    val departmentId: String,
    val departmentName: String,
    val date: String,
    val timeSlot: String,
    val duration: Int,
    val status: String,
    val reminderSet: Boolean = false,
    val createTime: Long = 0
)

data class BookReqDto(
    val doctorId: String,
    val date: String,
    val timeSlot: String,
    val duration: Int,
    val patientId: String,
    val patientName: String
)

data class RecommendQuery(
    val symptoms: String,
    val dept: String? = null
)

data class TriageBookReqDto(val symptoms: String)

data class DoctorRecDto(
    val doctor: DoctorDto,
    val score: Float
)

/** Retrofit endpoint surface — separate from the Android-side interface. */
interface AppointmentApiServiceRetrofit {

    @GET("api/departments")
    suspend fun listDepartments(@Query("parentId") parentId: String?): ApiEnvelope<List<DepartmentDto>>

    @GET("api/departments/all")
    suspend fun listAllDepartments(): ApiEnvelope<List<DepartmentDto>>

    @GET("api/doctors")
    suspend fun listDoctors(@Query("departmentId") departmentId: String): ApiEnvelope<List<DoctorDto>>

    @GET("api/doctors/{id}")
    suspend fun getDoctor(@Path("id") id: String): ApiEnvelope<DoctorDto>

    @GET("api/doctors/recommend")
    suspend fun recommend(
        @Query("symptoms") symptoms: String,
        @Query("dept") dept: String?
    ): ApiEnvelope<List<DoctorRecDto>>

    @POST("api/doctors/recommend-and-book")
    suspend fun recommendAndBook(@Body req: TriageBookReqDto): ApiEnvelope<AppointmentDto>

    @POST("api/appointments")
    suspend fun book(@Body req: BookReqDto): ApiEnvelope<AppointmentDto>

    @POST("api/appointments/{id}/cancel")
    suspend fun cancel(@Path("id") id: String): ApiEnvelope<Unit>

    @POST("api/appointments/{id}/pay")
    suspend fun pay(@Path("id") id: String): ApiEnvelope<Unit>

    @POST("api/appointments/{id}/no-show")
    suspend fun noShow(@Path("id") id: String): ApiEnvelope<Unit>

    @POST("api/appointments/{id}/reminder")
    suspend fun setReminder(@Path("id") id: String, @Query("on") on: Boolean): ApiEnvelope<Unit>

    @GET("api/appointments/mine")
    suspend fun mine(): ApiEnvelope<List<AppointmentDto>>

    @GET("api/appointments/no-show-count")
    suspend fun noShowCount(): ApiEnvelope<Int>
}