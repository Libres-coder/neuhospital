package com.example.neusoft_hospital.feature.preconsult.data

import com.example.neusoft_hospital.feature.auth.domain.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * After connecting to the real backend, triage is delegated to the server-side
 * [TriageService] (which holds the same rule set the Android client used to).
 * The Android client no longer needs the local [TriagePolicy] class.
 */
@Singleton
class PreConsultRepository @Inject constructor(
    private val api: PreConsultApiServiceRetrofit
) {
    suspend fun triage(symptoms: List<String>): Result<TriageResult> = runCatching {
        val resp = api.triage(TriageRequestDto(symptoms))
        if (resp.code != 0) error(resp.message ?: "triage failed")
        toResult(resp.data)
    }

    /**
     * Multipart image triage. Pass the raw bytes (e.g. from a content URI) +
     * an optional list of text symptoms. The server stores the upload, hands
     * the URL to qwen-vl-plus, and merges the description with the text.
     */
    suspend fun triageImage(
        bytes: ByteArray,
        mimeType: String,
        symptoms: List<String>
    ): Result<TriageResult> = runCatching {
        val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", "symptom.jpg", body)
        val symptomParts = symptoms.map {
            RequestBody.create("text/plain".toMediaTypeOrNull(), it)
        }
        val resp = api.triageImage(filePart, symptomParts)
        if (resp.code != 0) error(resp.message ?: "image triage failed")
        toResult(resp.data)
    }

    private fun toResult(data: TriageResponseDto?): TriageResult {
        val d = data ?: error("empty triage response")
        val diseases = d.possibleDiseases.orEmpty().map { m ->
            DiseaseSuggestion(
                name = (m["name"] as? String).orEmpty(),
                probability = ((m["probability"] as? Number)?.toFloat()) ?: 0f,
                description = (m["description"] as? String).orEmpty()
            )
        }
        val depts = d.recommendedDepartments.orEmpty().mapNotNull { m ->
            val name = m["departmentName"] as? String ?: return@mapNotNull null
            val conf = ((m["confidence"] as? Number)?.toFloat()) ?: 0f
            DepartmentRecommendation(Department(id = name, parentId = null, name = name, namePy = "", desc = ""), conf)
        }
        return TriageResult(diseases, depts)
    }
}