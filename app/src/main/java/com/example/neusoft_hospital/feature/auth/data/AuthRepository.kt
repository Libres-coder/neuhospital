package com.example.neusoft_hospital.feature.auth.data

import com.example.neusoft_hospital.core.data.local.dao.FamilyMemberDao
import com.example.neusoft_hospital.core.data.local.entity.FamilyMemberEntity
import com.example.neusoft_hospital.core.data.prefs.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApiService,
    private val prefs: UserPreferences,
    private val familyDao: FamilyMemberDao
) {
    suspend fun sendSms(phone: String): Result<String> = api.sendSms(phone)

    suspend fun verifySms(phone: String, code: String): Result<LoginResponse> {
        val resp = api.verifySms(phone, code)
        resp.onSuccess { r ->
            prefs.saveLogin(
                token = r.token,
                refreshToken = r.refreshToken ?: "",
                userId = r.userId,
                name = r.name,
                phone = r.phone
            )
            // add self as default family member
            familyDao.insert(FamilyMemberEntity(
                id = r.userId,
                ownerId = r.userId,
                name = r.name,
                phone = r.phone,
                idCard = "",
                relation = "本人",
                isDefault = true
            ))
            prefs.setCurrentPatient(r.userId)
        }
        return resp
    }

    suspend fun logout() {
        api.logout()
        prefs.logout()
    }

    suspend fun verifyIdCard(name: String, idCard: String): Result<Boolean> {
        val ok = api.verifyIdCard(name, idCard)
        if (ok.isSuccess && ok.getOrNull() == true) prefs.setVerified()
        return ok
    }

    suspend fun bindEhsCard(userId: String, cardNo: String): Result<String> {
        val resp = api.bindEhsCard(userId, cardNo)
        if (resp.isSuccess) prefs.setEhsBound()
        return resp
    }

    fun getFamilyMembers(): Flow<List<FamilyMemberEntity>> {
        return kotlinx.coroutines.flow.flow {
            val ownerId = prefs.tokenFlow.first()
            familyDao.getByOwner(ownerId).collect { emit(it) }
        }
    }

    suspend fun addFamilyMember(
        ownerId: String,
        name: String,
        phone: String,
        idCard: String,
        relation: String
    ): Result<FamilyMemberEntity> {
        val entity = FamilyMemberEntity(
            id = UUID.randomUUID().toString(),
            ownerId = ownerId,
            name = name,
            phone = phone,
            idCard = idCard,
            relation = relation
        )
        familyDao.insert(entity)
        return Result.success(entity)
    }

    suspend fun updateFamilyMember(entity: FamilyMemberEntity) = familyDao.update(entity)

    suspend fun deleteFamilyMember(entity: FamilyMemberEntity) = familyDao.delete(entity)

    suspend fun setDefaultMember(memberId: String, ownerId: String) {
        familyDao.clearDefault(ownerId)
        familyDao.setDefault(memberId)
        prefs.setCurrentPatient(memberId)
    }

    fun isLoggedIn(): Flow<Boolean> = prefs.isLoggedInFlow
    fun isVerified(): Flow<Boolean> = prefs.isVerifiedFlow
    fun hasEhs(): Flow<Boolean> = prefs.hasEhsFlow
    fun getCurrentPatientId(): Flow<String> = prefs.currentPatientIdFlow
    fun getUserName(): Flow<String> = prefs.userNameFlow
}
