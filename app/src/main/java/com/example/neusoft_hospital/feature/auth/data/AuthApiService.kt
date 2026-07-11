package com.example.neusoft_hospital.feature.auth.data

import com.example.neusoft_hospital.core.network.ApiEnvelope
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Abstraction over the auth backend. Implemented by:
 *  - [MockAuthApi]      when [com.example.neusoft_hospital.core.network.ApiProvider.useMock] is true
 *  - Retrofit-generated when wired through [com.example.neusoft_hospital.core.di.NetworkModule]
 *
 * Methods return [Result] so callers don't need to know whether the call hit a mock or HTTP.
 */
interface AuthApiService {

    suspend fun sendSms(phone: String): Result<String>

    suspend fun verifySms(phone: String, code: String): Result<LoginResponse>

    suspend fun verifyIdCard(name: String, idCard: String): Result<Boolean>

    suspend fun bindEhsCard(userId: String, cardNo: String): Result<String>

    suspend fun bindMedicalInsurance(cardNo: String): Result<MedicalInsuranceDto>

    suspend fun logout(): Result<Unit>

    suspend fun me(): Result<MeResponse>

    /**
     * Exchange a refresh token for a new (access, refresh) pair. Server rotates
     * the refresh token (single-use), so callers MUST persist the new pair.
     */
    suspend fun refresh(refreshToken: String): Result<LoginResponse>
}

data class MeResponse(
    val userId: String,
    val phone: String,
    val name: String,
    val isVerified: Boolean,
    val hasEhsCard: Boolean,
    val hasMedicalInsurance: Boolean = false
)

data class MedicalInsuranceDto(
    val medicalInsuranceNo: String,
    val boundAt: Long
)

// --- Retrofit DTOs (server Result<T> envelope -> data class) ---
data class SmsReqDto(val phone: String)
data class SmsRespDto(val traceId: String, val ttlSeconds: Int)
data class LoginReqDto(val phone: String, val code: String)
data class IdCardReqDto(val name: String, val idCard: String)
data class BindEhsReqDto(val ehsCardNo: String)
data class BindMiReqDto(val medicalInsuranceNo: String)
data class IdCardAckDto(val verified: Boolean)
data class BindEhsAckDto(val ehsCardNo: String)
data class RefreshReqDto(val refreshToken: String)

/**
 * Real Retrofit implementation. Created by Hilt only when ApiProvider.useMock == false.
 * Lives in this file so the Retrofit signatures are colocated with the interface.
 */
interface AuthApiServiceRetrofit {
    @POST("api/auth/sms")
    suspend fun sendSms(@Body req: SmsReqDto): ApiEnvelope<SmsRespDto>

    @POST("api/auth/login")
    suspend fun login(@Body req: LoginReqDto): ApiEnvelope<LoginResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body req: RefreshReqDto): ApiEnvelope<LoginResponse>

    @POST("api/auth/verify-idcard")
    suspend fun verifyIdCard(@Body req: IdCardReqDto): ApiEnvelope<IdCardAckDto>

    @POST("api/auth/bind-ehs")
    suspend fun bindEhs(@Body req: BindEhsReqDto): ApiEnvelope<BindEhsAckDto>

    @POST("api/auth/bind-mi")
    suspend fun bindMedicalInsurance(@Body req: BindMiReqDto): ApiEnvelope<MedicalInsuranceDto>

    @POST("api/auth/logout")
    suspend fun logout(): ApiEnvelope<Unit>

    @GET("api/auth/me")
    suspend fun me(): ApiEnvelope<MeResponse>
}