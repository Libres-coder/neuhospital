package com.example.neusoft_hospital.feature.preconsult.data

import com.example.neusoft_hospital.core.network.ApiEnvelope
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

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

    /**
     * Image + optional text triage. The server's Qwen-VL model describes the
     * photo, merges with the user-supplied symptoms, then runs the existing
     * rule-based recommender on the combined text.
     */
    @Multipart
    @POST("api/preconsult/triage/image")
    suspend fun triageImage(
        @Part file: MultipartBody.Part,
        @Part("symptoms") symptoms: List<RequestBody>
    ): ApiEnvelope<TriageResponseDto>
}