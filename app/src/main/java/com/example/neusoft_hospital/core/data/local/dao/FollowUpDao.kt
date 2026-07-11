package com.example.neusoft_hospital.core.data.local.dao

import androidx.room.*
import com.example.neusoft_hospital.core.data.local.entity.FollowUpPlanEntity
import com.example.neusoft_hospital.core.data.local.entity.FollowUpTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowUpDao {
    @Query("SELECT * FROM follow_up_plans WHERE patientId = :patientId ORDER BY createTime DESC")
    fun getPlans(patientId: String): Flow<List<FollowUpPlanEntity>>

    @Query("SELECT * FROM follow_up_tasks WHERE planId = :planId ORDER BY dayIndex ASC")
    fun getTasksByPlan(planId: String): Flow<List<FollowUpTaskEntity>>

    @Query("SELECT * FROM follow_up_tasks WHERE patientId = :patientId AND completed = 0 AND targetDate <= :today ORDER BY targetDate ASC")
    fun getPendingTasks(patientId: String, today: String): Flow<List<FollowUpTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(entity: FollowUpPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<FollowUpTaskEntity>)

    @Update
    suspend fun updateTask(entity: FollowUpTaskEntity)

    @Query("UPDATE follow_up_tasks SET completed = 1, answers = :answers, completedTime = :time WHERE id = :id")
    suspend fun completeTask(id: String, answers: String, time: Long)
}

@Dao
interface RehabLogDao {
    @Query("SELECT * FROM rehab_logs WHERE patientId = :patientId ORDER BY date DESC")
    fun getLogs(patientId: String): Flow<List<com.example.neusoft_hospital.core.data.local.entity.RehabLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: com.example.neusoft_hospital.core.data.local.entity.RehabLogEntity)

    @Update
    suspend fun update(entity: com.example.neusoft_hospital.core.data.local.entity.RehabLogEntity)
}
