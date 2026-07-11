package com.example.neusoft_hospital.feature.preconsult.data

import com.example.neusoft_hospital.core.network.ApiEnvelope
import retrofit2.http.Body
import retrofit2.http.POST

/** Request/response DTOs for the backend triage endpoint. */
data class TriageRequestDto(val symptoms: List<String>)
data class TriageResponseDto(
    val possibleDiseases: List<Map<String, Any?>>?,
    val recommendedDepartments: List<Map<String, Any?>>?
)

/** Retrofit endpoint surface for the preconsult module. */
interface PreConsultApiServiceRetrofit {
    @POST("api/preconsult/triage")
    suspend fun triage(@Body req: TriageRequestDto): ApiEnvelope<TriageResponseDto>
}