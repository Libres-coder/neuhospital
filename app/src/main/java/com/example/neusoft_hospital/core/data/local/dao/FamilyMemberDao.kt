package com.example.neusoft_hospital.core.data.local.dao

import androidx.room.*
import com.example.neusoft_hospital.core.data.local.entity.FamilyMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members WHERE ownerId = :ownerId ORDER BY isDefault DESC, name ASC")
    fun getByOwner(ownerId: String): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE id = :id")
    suspend fun getById(id: String): FamilyMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FamilyMemberEntity)

    @Update
    suspend fun update(entity: FamilyMemberEntity)

    @Delete
    suspend fun delete(entity: FamilyMemberEntity)

    @Query("UPDATE family_members SET isDefault = 0 WHERE ownerId = :ownerId")
    suspend fun clearDefault(ownerId: String)

    @Query("UPDATE family_members SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: String)
}
