package com.example.neusoft_hospital.core.data.local.dao

import androidx.room.*
import com.example.neusoft_hospital.core.data.local.entity.AppointmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments WHERE patientId = :patientId ORDER BY createTime DESC")
    fun getByPatient(patientId: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE patientId = :patientId AND date >= :today AND status IN ('pending','payed','confirmed') ORDER BY date ASC")
    fun getUpcoming(patientId: String, today: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE date >= :today AND status IN ('pending','payed','confirmed') ORDER BY date ASC")
    fun getUpcoming(today: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getById(id: String): AppointmentEntity?

    @Query("SELECT * FROM appointments ORDER BY createTime DESC")
    fun getAll(): Flow<List<AppointmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AppointmentEntity)

    @Update
    suspend fun update(entity: AppointmentEntity)

    @Query("UPDATE appointments SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE appointments SET reminderSet = :set WHERE id = :id")
    suspend fun setReminder(id: String, set: Boolean)

    @Query("DELETE FROM appointments WHERE patientId = :patientId")
    suspend fun deleteByPatient(patientId: String)

    @Query("SELECT COUNT(*) FROM appointments WHERE patientId = :patientId AND status = 'no_show'")
    suspend fun getNoShowCount(patientId: String): Int
}
