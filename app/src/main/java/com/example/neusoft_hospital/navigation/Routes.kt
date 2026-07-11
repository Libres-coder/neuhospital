package com.example.neusoft_hospital.navigation

sealed class Routes(val route: String) {
    // Auth
    data object Splash : Routes("splash")
    data object Login : Routes("login")
    data object Register : Routes("register")
    data object Verify : Routes("verify")
    data object FamilyManage : Routes("family_manage")
    data object FamilyAdd : Routes("family_add?memberId={memberId}") {
        fun create(memberId: String? = null) = "family_add?memberId=$memberId"
    }

    // Main
    data object Main : Routes("main")
    data object Profile : Routes("profile")

    // Appointment
    data object AppointmentHome : Routes("appointment_home")
    data object DepartmentList : Routes("department_list?parentId={parentId}") {
        fun create(parentId: String? = null) = "department_list?parentId=$parentId"
    }
    data object DoctorList : Routes("doctor_list?departmentId={departmentId}") {
        fun create(departmentId: String) = "doctor_list?departmentId=$departmentId"
    }
    data object DoctorDetail : Routes("doctor_detail?doctorId={doctorId}") {
        fun create(doctorId: String) = "doctor_detail?doctorId=$doctorId"
    }
    data object Schedule : Routes("schedule?doctorId={doctorId}") {
        fun create(doctorId: String) = "schedule?doctorId=$doctorId"
    }
    data object Booking : Routes("booking?doctorId={doctorId}&date={date}&slotId={slotId}") {
        fun create(doctorId: String, date: String, slotId: String) =
            "booking?doctorId=$doctorId&date=$date&slotId=$slotId"
    }
    data object MyAppointments : Routes("my_appointments")
    data object SmartRecommend : Routes("smart_recommend?symptoms={symptoms}") {
        fun create(symptoms: String = "") = "smart_recommend?symptoms=$symptoms"
    }

    // PreConsult
    data object PreConsultHome : Routes("preconsult_home")
    data object SymptomInput : Routes("symptom_input?mode={mode}") {
        fun create(mode: String = "text") = "symptom_input?mode=$mode"
    }
    data object TriageResult : Routes("triage_result?symptoms={symptoms}") {
    fun create(symptoms: String = "") = "triage_result?symptoms=${java.net.URLEncoder.encode(symptoms, "UTF-8")}"
}

    // AI Chat
    data object AiChat : Routes("ai_chat?sessionId={sessionId}") {
        fun create(sessionId: String? = null) = "ai_chat?sessionId=$sessionId"
    }
    data object ChatHistory : Routes("chat_history")

    // FollowUp
    data object FollowUpHome : Routes("followup_home")
    data object FollowUpPlan : Routes("followup_plan?planId={planId}") {
        fun create(planId: String) = "followup_plan?planId=$planId"
    }
    data object RehabGuide : Routes("rehab_guide?disease={disease}") {
        fun create(disease: String) = "rehab_guide?disease=$disease"
    }
    data object ChronicDashboard : Routes("chronic_dashboard?type={type}") {
        fun create(type: String) = "chronic_dashboard?type=$type"
    }
    data object ChronicInput : Routes("chronic_input?type={type}") {
        fun create(type: String) = "chronic_input?type=$type"
    }
    data object ChronicAlerts : Routes("chronic_alerts")
}
