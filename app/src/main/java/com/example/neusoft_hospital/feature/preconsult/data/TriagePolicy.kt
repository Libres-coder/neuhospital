package com.example.neusoft_hospital.feature.preconsult.data

import com.example.neusoft_hospital.feature.auth.domain.Department
import com.example.neusoft_hospital.feature.auth.domain.DepartmentRecommendation
import com.example.neusoft_hospital.feature.auth.domain.DiseaseSuggestion
import com.example.neusoft_hospital.feature.auth.domain.TriageResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本地规则 + 远端 Qwen 兜底
 */
@Singleton
class TriagePolicy @Inject constructor() {

    private val symptomMap = mapOf(
        "胸痛" to listOf("心血管内科" to 0.9f, "呼吸内科" to 0.5f),
        "心悸" to listOf("心血管内科" to 0.95f),
        "高血压" to listOf("心血管内科" to 0.95f),
        "气短" to listOf("呼吸内科" to 0.7f, "心血管内科" to 0.6f),
        "咳嗽" to listOf("呼吸内科" to 0.8f),
        "哮喘" to listOf("呼吸内科" to 0.95f),
        "胃痛" to listOf("消化内科" to 0.9f),
        "腹泻" to listOf("消化内科" to 0.85f),
        "骨折" to listOf("骨科" to 0.95f),
        "关节" to listOf("骨科" to 0.85f),
        "皮疹" to listOf("皮肤科" to 0.85f),
        "过敏" to listOf("皮肤科" to 0.7f, "耳鼻喉科" to 0.5f),
        "视力" to listOf("眼科" to 0.9f),
        "眼红" to listOf("眼科" to 0.85f),
        "牙痛" to listOf("口腔科" to 0.9f),
        "耳鸣" to listOf("耳鼻喉科" to 0.85f),
        "失眠" to listOf("中医科" to 0.6f, "心血管内科" to 0.4f),
        "焦虑" to listOf("中医科" to 0.5f),
        "儿童" to listOf("儿科" to 0.95f),
        "发热" to listOf("内科" to 0.7f, "儿科" to 0.85f),
        "腹痛" to listOf("消化内科" to 0.8f, "普通外科" to 0.6f),
    )

    fun recommendBySymptoms(symptoms: List<String>, departments: List<Department>): List<DepartmentRecommendation> {
        val scores = mutableMapOf<String, Float>()
        symptoms.forEach { symptom ->
            symptomMap.forEach { (key, list) ->
                if (symptom.contains(key) || key.contains(symptom)) {
                    list.forEach { (deptName, score) ->
                        val dept = departments.firstOrNull { it.name == deptName } ?: return@forEach
                        scores[dept.id] = (scores[dept.id] ?: 0f) + score
                    }
                }
            }
        }
        // fallback to 内科 if nothing matched
        if (scores.isEmpty()) {
            departments.firstOrNull { it.name == "内科" }?.let { scores[it.id] = 0.5f }
        }
        return scores.entries.sortedByDescending { it.value }.take(3).map { entry ->
            val dept = departments.first { it.id == entry.key }
            DepartmentRecommendation(dept, (entry.value / symptoms.size.coerceAtLeast(1)).coerceAtMost(1.0f))
        }
    }

    fun possibleDiseases(symptoms: List<String>): List<DiseaseSuggestion> {
        val matched = symptoms.mapNotNull { symptom ->
            symptomMap.entries.firstOrNull { symptom.contains(it.key) || it.key.contains(symptom) }?.key
        }
        val diseases = matched.map {
            DiseaseSuggestion(it, 0.6f, "症状与\"$it\"较为相关，建议尽快就医确诊。")
        }
        if (diseases.isEmpty()) {
            return listOf(DiseaseSuggestion("暂无明确匹配", 0.0f, "建议描述更具体的症状或咨询医生"))
        }
        return diseases
    }
}