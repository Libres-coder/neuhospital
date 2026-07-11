package com.example.neusoft_hospital.feature.auth.data

import com.example.neusoft_hospital.core.network.ApiEnvelope
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Family member / 亲情账户 API. Either [MockFamilyApi] (offline) or
 * [FamilyApiServiceRetrofit] (real server) implements this.
 *
 * The server is the source of truth in non-mock mode: list/add/update/delete
 * all hit `/api/family/..`, then on success the Android cache (Room) is
 * refreshed so the UI stays reactive.
 */
interface FamilyApiService {
    suspend fun list(): Result<FamilyListDto>
    suspend fun add(req: FamilyAddReqDto): Result<FamilyMemberDto>
    suspend fun update(id: String, req: FamilyUpdateReqDto): Result<FamilyMemberDto>
    suspend fun delete(id: String): Result<Unit>
    suspend fun setDefault(id: String): Result<FamilyMemberDto>
}

// ---- DTOs ----
data class FamilyListDto(
    val members: List<FamilyMemberDto>,
    val currentPatientId: String
)

data class FamilyMemberDto(
    val id: String,
    val ownerId: String,
    val name: String,
    val phone: String,
    val idCard: String,
    val relation: String,
    val avatar: String? = null,
    val isDefault: Boolean = false,
    val createTime: Long = 0,
    val updateTime: Long? = null
)

data class FamilyAddReqDto(
    val name: String,
    val phone: String,
    val idCard: String = "",
    val relation: String,
    val avatar: String? = null
)

data class FamilyUpdateReqDto(
    val name: String? = null,
    val phone: String? = null,
    val idCard: String? = null,
    val relation: String? = null,
    val avatar: String? = null
)

// ---- Retrofit ----
interface FamilyApiServiceRetrofit {
    @GET("api/family")
    suspend fun list(): ApiEnvelope<FamilyListDto>

    @POST("api/family")
    suspend fun add(@Body req: FamilyAddReqDto): ApiEnvelope<FamilyMemberDto>

    @PATCH("api/family/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body req: FamilyUpdateReqDto
    ): ApiEnvelope<FamilyMemberDto>

    @DELETE("api/family/{id}")
    suspend fun delete(@Path("id") id: String): ApiEnvelope<FamilyDeleteAckDto>

    @POST("api/family/{id}/default")
    suspend fun setDefault(@Path("id") id: String): ApiEnvelope<FamilyMemberDto>
}

// Server returns Unit for delete; wrap in a typed ack so callers don't have
// to know about Retrofit's Unit marshalling.
data class FamilyDeleteAckDto(val deleted: Boolean = true)
