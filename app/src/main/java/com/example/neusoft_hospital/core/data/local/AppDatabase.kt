package com.example.neusoft_hospital.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.neusoft_hospital.core.data.local.dao.*
import com.example.neusoft_hospital.core.data.local.entity.*

@Database(
    entities = [
        AppointmentEntity::class,
        FamilyMemberEntity::class,
        FollowUpPlanEntity::class,
        FollowUpTaskEntity::class,
        RehabLogEntity::class,
        ChronicRecordEntity::class,
        ChronicAlertEntity::class,
        ChatSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appointmentDao(): AppointmentDao
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun followUpDao(): FollowUpDao
    abstract fun rehabLogDao(): RehabLogDao
    abstract fun chronicDao(): ChronicDao
    abstract fun chatSessionDao(): ChatSessionDao
}
