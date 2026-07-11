package com.example.neusoft_hospital.core.data.local.dao

import androidx.room.*
import com.example.neusoft_hospital.core.data.local.entity.ChatSessionEntity
import com.example.neusoft_hospital.core.data.local.entity.ChronicAlertEntity
import com.example.neusoft_hospital.core.data.local.entity.ChronicRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChronicDao {
    @Query("SELECT * FROM chronic_records WHERE patientId = :patientId AND type = :type ORDER BY date DESC")
    fun getRecords(patientId: String, type: String): Flow<List<ChronicRecordEntity>>

    @Query("SELECT * FROM chronic_records WHERE patientId = :patientId ORDER BY date DESC LIMIT :limit")
    fun getRecentRecords(patientId: String, limit: Int = 7): Flow<List<ChronicRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(entity: ChronicRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(entity: ChronicAlertEntity)

    @Query("SELECT * FROM chronic_alerts WHERE patientId = :patientId AND acknowledged = 0 ORDER BY createTime DESC")
    fun getUnackedAlerts(patientId: String): Flow<List<ChronicAlertEntity>>

    @Query("UPDATE chronic_alerts SET acknowledged = 1 WHERE id = :id")
    suspend fun ackAlert(id: String)
}

@Dao
interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions WHERE patientId = :patientId ORDER BY lastTime DESC")
    fun getSessions(patientId: String): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getById(id: String): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ChatSessionEntity)

    @Update
    suspend fun update(entity: ChatSessionEntity)

    @Delete
    suspend fun delete(entity: ChatSessionEntity)
}
