package com.example.neusoft_hospital.feature.followup.data

import com.example.neusoft_hospital.core.network.ApiEnvelope
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class ChronicRecordRespDto(
    val id: String, val type: String, val date: String,
    val systolic: Int?, val diastolic: Int?, val heartRate: Int?,
    val fastingGlucose: Double?, val postprandialGlucose: Double?,
    val hba1c: Double?, val note: String?,
    val alertLevel: Int, val createTime: Long
)

data class ChronicAlertRespDto(
    val id: String, val recordId: String, val type: String,
    val level: Int, val message: String,
    val createTime: Long, val acknowledged: Boolean
)

data class SubmitChronicReqDto(
    val type: String, val date: String,
    val systolic: Int?, val diastolic: Int?, val heartRate: Int?,
    val fastingGlucose: Double?, val postprandialGlucose: Double?,
    val hba1c: Double?, val note: String?
)

interface ChronicApiServiceRetrofit {
    @GET("api/chronic/records")
    suspend fun listRecords(@Query("type") type: String): ApiEnvelope<List<ChronicRecordRespDto>>

    @POST("api/chronic/records")
    suspend fun submit(@Body req: SubmitChronicReqDto): ApiEnvelope<ChronicRecordRespDto>

    @GET("api/chronic/alerts")
    suspend fun alerts(): ApiEnvelope<List<ChronicAlertRespDto>>

    @POST("api/chronic/alerts/{id}/ack")
    suspend fun ack(@Path("id") id: String): ApiEnvelope<Unit>
}