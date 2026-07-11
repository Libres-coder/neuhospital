package com.example.neusoft_hospital.feature.preconsult.data

import com.example.neusoft_hospital.feature.auth.domain.*
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
        val data = resp.data ?: error("empty triage response")
        val diseases = data.possibleDiseases.orEmpty().map { m ->
            DiseaseSuggestion(
                name = (m["name"] as? String).orEmpty(),
                probability = ((m["probability"] as? Number)?.toFloat()) ?: 0f,
                description = (m["description"] as? String).orEmpty()
            )
        }
        val depts = data.recommendedDepartments.orEmpty().mapNotNull { m ->
            val name = m["departmentName"] as? String ?: return@mapNotNull null
            val conf = ((m["confidence"] as? Number)?.toFloat()) ?: 0f
            DepartmentRecommendation(Department(id = name, parentId = null, name = name, namePy = "", desc = ""), conf)
        }
        TriageResult(diseases, depts)
    }
}