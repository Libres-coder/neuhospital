package com.example.neusoft_hospital.feature.auth.domain

data class Department(
    val id: String,
    val parentId: String?,
    val name: String,
    val namePy: String, // pinyin for search
    val desc: String,
    val iconUrl: String? = null
)

data class Doctor(
    val id: String,
    val name: String,
    val departmentId: String,
    val departmentName: String,
    val title: String, // 主任医师/副主任医师/主治医师
    val expertise: String, // 擅长领域
    val profile: String, // 个人简介
    val rating: Float, // 4.5
    val avatarUrl: String? = null,
    val schedule: List<Schedule> = emptyList()
)

data class Schedule(
    val date: String,
    val dayOfWeek: String,
    val slots: List<TimeSlot> = emptyList()
)

data class TimeSlot(
    val id: String,
    val startTime: String, // "08:00"
    val endTime: String,   // "08:15"
    val available: Int,    // remaining count
    val total: Int
)

data class Appointment(
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
    val status: AppointmentStatus,
    val createTime: Long
)

enum class AppointmentStatus {
    PENDING, PAYED, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW
}

data class DoctorRecommendation(
    val doctor: Doctor,
    val score: Float,
    val reasons: List<String> // e.g. ["专业方向匹配", "历史好评率高"]
)

data class TriageResult(
    val possibleDiseases: List<DiseaseSuggestion>,
    val recommendedDepartments: List<DepartmentRecommendation>
)

data class DiseaseSuggestion(
    val name: String,
    val probability: Float, // 0.0-1.0
    val description: String
)

data class DepartmentRecommendation(
    val department: Department,
    val confidence: Float
)

data class ChronicRecord(
    val id: String,
    val patientId: String,
    val type: ChronicType,
    val date: String,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val heartRate: Int? = null,
    val fastingGlucose: Float? = null,
    val postprandialGlucose: Float? = null,
    val hba1c: Float? = null,
    val note: String? = null,
    val alertLevel: Int = 0
)

enum class ChronicType { HYPERTENSION, DIABETES }

data class ChronicAlert(
    val id: String,
    val recordId: String,
    val type: ChronicType,
    val level: Int,
    val message: String,
    val createTime: Long
)

data class FollowUpPlan(
    val id: String,
    val patientId: String,
    val disease: String,
    val surgeryDate: String,
    val totalDays: Int,
    val tasks: List<FollowUpTask> = emptyList()
)

data class FollowUpTask(
    val id: String,
    val planId: String,
    val dayIndex: Int,
    val targetDate: String,
    val questions: List<String>,
    val answers: Map<String, String>? = null,
    val completed: Boolean = false,
    val doctorReply: String? = null
)

data class FamilyMember(
    val id: String,
    val name: String,
    val phone: String,
    val idCard: String,
    val relation: String,
    val avatar: String? = null,
    val isDefault: Boolean = false
)

data class ChatMessage(
    val id: String,
    val sessionId: String,
    val role: String, // user / assistant
    val content: String,
    val time: Long
)

data class ChatSession(
    val id: String,
    val title: String,
    val lastMessage: String,
    val lastTime: Long
)
