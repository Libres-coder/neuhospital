package com.example.neusoft_hospital.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val patientName: String,
    val doctorId: String,
    val doctorName: String,
    val departmentId: String,
    val departmentName: String,
    val date: String,
    val timeSlot: String,
    val duration: Int, // minutes: 15/30/45/60
    val status: String, // pending/payed/confirmed/completed/cancelled/no_show
    val createTime: Long,
    val reminderSet: Boolean = false
)

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val name: String,
    val phone: String,
    val idCard: String,
    val relation: String, // 父母/子女/配偶/其他
    val avatar: String? = null,
    val isDefault: Boolean = false
)

@Entity(tableName = "follow_up_plans")
data class FollowUpPlanEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val disease: String,
    val surgeryDate: String,
    val totalDays: Int,
    val createTime: Long
)

@Entity(tableName = "follow_up_tasks")
data class FollowUpTaskEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val patientId: String,
    val dayIndex: Int,
    val targetDate: String,
    val questions: String, // JSON: ["伤口愈合情况","疼痛评分","用药情况"...]
    val answers: String? = null, // JSON answered
    val completed: Boolean = false,
    val completedTime: Long? = null,
    val doctorReply: String? = null
)

@Entity(tableName = "rehab_logs")
data class RehabLogEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val disease: String,
    val date: String,
    val items: String, // JSON: ["Day1-动作A","Day1-动作B"...]
    val completed: Boolean = false
)

@Entity(tableName = "chronic_records")
data class ChronicRecordEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val type: String, // hypertension / diabetes
    val date: String,
    val systolic: Int? = null,   // 高血压收缩压
    val diastolic: Int? = null,  // 高血压舒张压
    val heartRate: Int? = null, // 心率
    val fastingGlucose: Float? = null,  // 空腹血糖
    val postprandialGlucose: Float? = null, // 餐后血糖
    val hba1c: Float? = null,   // 糖化血红蛋白
    val note: String? = null,
    val alertLevel: Int = 0 // 0=正常 1=注意 2=警告 3=危险
)

@Entity(tableName = "chronic_alerts")
data class ChronicAlertEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val recordId: String,
    val type: String,
    val level: Int,
    val message: String,
    val createTime: Long,
    val acknowledged: Boolean = false
)

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val title: String,
    val createTime: Long,
    val lastTime: Long,
    val lastMessage: String = ""
)
