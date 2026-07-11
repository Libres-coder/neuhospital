package com.example.neusoft_hospital.feature.auth.data

import com.example.neusoft_hospital.core.network.ApiProvider
import kotlinx.coroutines.delay
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockFamilyApi @Inject constructor() : FamilyApiService {

    private val store: MutableMap<String, FamilyMemberDto> = LinkedHashMap()
    private var currentPatientId: String = ""

    init {
        // seed self
        val selfId = "fm_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
        val self = FamilyMemberDto(
            id = selfId,
            ownerId = "self",
            name = "本人",
            phone = "13800000000",
            idCard = "",
            relation = "本人",
            isDefault = true
        )
        store[selfId] = self
        currentPatientId = selfId
    }

    override suspend fun list(): Result<FamilyListDto> {
        delay(ApiProvider.MOCK_DELAY_MS)
        return Result.success(FamilyListDto(
            members = store.values.toList(),
            currentPatientId = currentPatientId
        ))
    }

    override suspend fun add(req: FamilyAddReqDto): Result<FamilyMemberDto> {
        delay(ApiProvider.MOCK_DELAY_MS)
        val id = "fm_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
        val m = FamilyMemberDto(
            id = id,
            ownerId = "self",
            name = req.name,
            phone = req.phone,
            idCard = req.idCard,
            relation = req.relation,
            avatar = req.avatar,
            isDefault = false
        )
        store[id] = m
        return Result.success(m)
    }

    override suspend fun update(id: String, req: FamilyUpdateReqDto): Result<FamilyMemberDto> {
        delay(ApiProvider.MOCK_DELAY_MS)
        val cur = store[id] ?: return Result.failure(Exception("not found"))
        val next = cur.copy(
            name = req.name ?: cur.name,
            phone = req.phone ?: cur.phone,
            idCard = req.idCard ?: cur.idCard,
            relation = req.relation ?: cur.relation,
            avatar = req.avatar ?: cur.avatar
        )
        store[id] = next
        return Result.success(next)
    }

    override suspend fun delete(id: String): Result<Unit> {
        delay(ApiProvider.MOCK_DELAY_MS)
        if (store[id]?.isDefault == true && store.size == 1) {
            return Result.failure(Exception("至少需要保留一位亲情账户"))
        }
        store.remove(id)
        if (currentPatientId == id) {
            currentPatientId = store.values.first().id
            store[currentPatientId] = store.getValue(currentPatientId).copy(isDefault = true)
        }
        return Result.success(Unit)
    }

    override suspend fun setDefault(id: String): Result<FamilyMemberDto> {
        delay(ApiProvider.MOCK_DELAY_MS)
        val target = store[id] ?: return Result.failure(Exception("not found"))
        val cleared = store.mapValues { (k, v) ->
            if (k == id) v.copy(isDefault = true) else v.copy(isDefault = false)
        }
        store.clear()
        store.putAll(cleared)
        currentPatientId = id
        return Result.success(store.getValue(id))
    }
}
