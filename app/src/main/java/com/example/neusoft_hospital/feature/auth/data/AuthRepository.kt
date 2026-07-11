package com.example.neusoft_hospital.feature.auth.data

import com.example.neusoft_hospital.core.data.local.dao.FamilyMemberDao
import com.example.neusoft_hospital.core.data.local.entity.FamilyMemberEntity
import com.example.neusoft_hospital.core.data.prefs.UserPreferences
import com.example.neusoft_hospital.core.network.ApiProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApiService,
    private val familyApi: FamilyApiService,
    private val prefs: UserPreferences,
    private val familyDao: FamilyMemberDao
) {
    /** Application-scoped scope used for fire-and-forget server refreshes. */
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun launchSafe(block: suspend () -> Unit) {
        backgroundScope.launch {
            try { block() } catch (_: Throwable) {}
        }
    }

    // helpers above; the rest stays the same

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
            // Pull the server-canonical family list (server auto-seeds the
            // self ("本人") member on first call). Replace local cache so a
            // fresh device immediately sees the same state.
            launchSafe {
                runCatching { refreshFamilyFromServer(r.userId) }
                    .onFailure {
                        // server may be offline in mock/dev — fall back to local insert
                        familyDao.insert(
                            FamilyMemberEntity(
                                id = r.userId,
                                ownerId = r.userId,
                                name = r.name,
                                phone = r.phone,
                                idCard = "",
                                relation = "本人",
                                isDefault = true
                            )
                        )
                        prefs.setCurrentPatient(r.userId)
                    }
            }
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

    suspend fun bindMedicalInsurance(cardNo: String): Result<MedicalInsuranceDto> {
        val resp = api.bindMedicalInsurance(cardNo)
        if (resp.isSuccess) prefs.setMiBound()
        return resp
    }

    /**
     * Reactive stream of family members. Backed by Room (offline-readable),
     * but we refresh from the server on first subscription and after every
     * mutation. If the server is unreachable and we're in real-mode, the
     * cached Room list is still shown.
     */
    fun getFamilyMembers(): Flow<List<FamilyMemberEntity>> = flow {
        val ownerId = prefs.tokenFlow.first()
        launchSafe { refreshFamilyFromServer(ownerId) }
        familyDao.getByOwner(ownerId).collect { emit(it) }
    }

    /**
     * Pull server-side list and write all rows into Room. Returns the
     * server's "current patient" id and persists it in preferences.
     */
    private suspend fun refreshFamilyFromServer(ownerId: String) {
        val resp = familyApi.list().getOrNull() ?: return
        val entities = resp.members.map { d ->
            FamilyMemberEntity(
                id = d.id,
                ownerId = d.ownerId,
                name = d.name,
                phone = d.phone,
                idCard = d.idCard,
                relation = d.relation,
                avatar = d.avatar,
                isDefault = d.isDefault
            )
        }
        // Wipe + re-insert so deleted-on-server rows disappear.
        familyDao.deleteAllForOwner(ownerId)
        entities.forEach { familyDao.insert(it) }
        if (resp.currentPatientId.isNotBlank()) {
            prefs.setCurrentPatient(resp.currentPatientId)
        }
    }

    suspend fun addFamilyMember(
        ownerId: String,
        name: String,
        phone: String,
        idCard: String,
        relation: String
    ): Result<FamilyMemberEntity> {
        val remote = familyApi.add(
            FamilyAddReqDto(
                name = name,
                phone = phone,
                idCard = idCard,
                relation = relation
            )
        )
        if (remote.isSuccess) {
            val dto = remote.getOrNull()!!
            val entity = FamilyMemberEntity(
                id = dto.id,
                ownerId = dto.ownerId,
                name = dto.name,
                phone = dto.phone,
                idCard = dto.idCard,
                relation = dto.relation,
                avatar = dto.avatar,
                isDefault = dto.isDefault
            )
            familyDao.insert(entity)
            return Result.success(entity)
        }
        // server unreachable / mock — keep the original local-only behavior
        if (ApiProvider.useMock) {
            val local = FamilyMemberEntity(
                id = UUID.randomUUID().toString(),
                ownerId = ownerId,
                name = name,
                phone = phone,
                idCard = idCard,
                relation = relation
            )
            familyDao.insert(local)
            return Result.success(local)
        }
        return Result.failure(remote.exceptionOrNull() ?: Exception("add failed"))
    }

    suspend fun updateFamilyMember(entity: FamilyMemberEntity): Result<FamilyMemberEntity> {
        // Optimistic local update so UI updates instantly.
        familyDao.update(entity)
        val remote = familyApi.update(
            entity.id,
            FamilyUpdateReqDto(
                name = entity.name,
                phone = entity.phone,
                idCard = entity.idCard,
                relation = entity.relation,
                avatar = entity.avatar
            )
        )
        return if (remote.isSuccess) Result.success(entity) else Result.failure(remote.exceptionOrNull() ?: Exception("update failed"))
    }

    suspend fun deleteFamilyMember(entity: FamilyMemberEntity): Result<Unit> {
        val remote = familyApi.delete(entity.id)
        if (remote.isSuccess) familyDao.delete(entity)
        return remote
    }

    suspend fun setDefaultMember(memberId: String, ownerId: String) {
        familyApi.setDefault(memberId).onSuccess {
            refreshFamilyFromServer(ownerId)
        }
        prefs.setCurrentPatient(memberId)
    }

    fun isLoggedIn(): Flow<Boolean> = prefs.isLoggedInFlow
    fun isVerified(): Flow<Boolean> = prefs.isVerifiedFlow
    fun hasEhs(): Flow<Boolean> = prefs.hasEhsFlow
    fun getCurrentPatientId(): Flow<String> = prefs.currentPatientIdFlow
    fun getUserName(): Flow<String> = prefs.userNameFlow
}
