package com.example.neusoft_hospital.feature.followup.data

import com.example.neusoft_hospital.core.network.ApiEnvelope
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class PlanRespDto(
    val id: String, val patientId: String?, val disease: String,
    val surgeryDate: String, val totalDays: Int, val createTime: Long,
    val tasks: List<TaskRespDto>?
)

data class TaskRespDto(
    val id: String, val planId: String, val dayIndex: Int, val targetDate: String,
    val questions: List<String>?, val answers: Map<String, String>?,
    val completed: Boolean, val completedTime: Long?, val doctorReply: String?
)

data class CreatePlanReqDto(val disease: String, val surgeryDate: String, val totalDays: Int)
data class CompleteTaskReqDto(val taskId: String, val answers: Map<String, String>)

data class RehabLogRespDto(
    val id: String, val disease: String, val logDate: String,
    val items: List<String>, val completed: Boolean
)

data class CreateRehabReqDto(val disease: String, val items: List<String>)

interface FollowUpApiServiceRetrofit {
    @POST("api/followup/plans")
    suspend fun createPlan(@Body req: CreatePlanReqDto): ApiEnvelope<PlanRespDto>

    @GET("api/followup/plans")
    suspend fun listPlans(): ApiEnvelope<List<PlanRespDto>>

    @GET("api/followup/plans/{id}/tasks")
    suspend fun listTasks(@Path("id") id: String): ApiEnvelope<List<TaskRespDto>>

    @GET("api/followup/tasks/pending")
    suspend fun pendingTasks(): ApiEnvelope<List<TaskRespDto>>

    @POST("api/followup/tasks/complete")
    suspend fun completeTask(@Body req: CompleteTaskReqDto): ApiEnvelope<Unit>

    @GET("api/rehab/logs")
    suspend fun listRehab(): ApiEnvelope<List<RehabLogRespDto>>

    @POST("api/rehab/logs")
    suspend fun createRehab(@Body req: CreateRehabReqDto): ApiEnvelope<RehabLogRespDto>

    @POST("api/rehab/logs/{id}/complete")
    suspend fun completeRehab(@Path("id") id: String): ApiEnvelope<Unit>
}