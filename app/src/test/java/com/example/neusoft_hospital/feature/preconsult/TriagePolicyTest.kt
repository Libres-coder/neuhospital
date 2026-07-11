package com.example.neusoft_hospital.feature.preconsult

import com.example.neusoft_hospital.feature.auth.domain.ChronicType
import com.example.neusoft_hospital.feature.followup.data.ChronicRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TriagePolicyTest {
    private val policy = com.example.neusoft_hospital.feature.preconsult.data.TriagePolicy()

    @Test fun `hypertension symptom maps to cardiovascular`() {
        val recs = policy.recommendBySymptoms(listOf("高血压", "头痛"), listOf(
            com.example.neusoft_hospital.feature.auth.domain.Department("d1_1", "d1", "心血管内科", "xinxueguanneike", "高血压")
        ))
        assertTrue(recs.isNotEmpty())
        assertEquals("心血管内科", recs.first().department.name)
    }

    @Test fun `empty symptoms falls back to internal medicine`() {
        val recs = policy.recommendBySymptoms(listOf("xyz"), listOf(
            com.example.neusoft_hospital.feature.auth.domain.Department("d1", null, "内科", "neike", "内科")
        ))
        assertEquals("内科", recs.first().department.name)
    }

    @Test fun `chest pain prioritizes cardiology with high confidence`() {
        val recs = policy.recommendBySymptoms(listOf("胸痛"), listOf(
            com.example.neusoft_hospital.feature.auth.domain.Department("d1_1", "d1", "心血管内科", "xinxueguanneike", ""),
            com.example.neusoft_hospital.feature.auth.domain.Department("d1_2", "d1", "呼吸内科", "huxineike", "")
        ))
        assertEquals("心血管内科", recs.first().department.name)
        assertTrue(recs.first().confidence > 0.5f)
    }
}