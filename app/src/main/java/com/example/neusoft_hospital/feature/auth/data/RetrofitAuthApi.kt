package com.example.neusoft_hospital.feature.auth.data

import com.example.neusoft_hospital.core.util.SmsSimulator
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-network adapter. Wraps the Retrofit-generated [AuthApiServiceRetrofit]
 * and surfaces results as [Result] so it has the exact same shape as [MockAuthApi].
 */
@Singleton
class RetrofitAuthApi @Inject constructor(
    private val service: AuthApiServiceRetrofit
) : AuthApiService {

    override suspend fun sendSms(phone: String): Result<String> = runCatching {
        val resp = service.sendSms(SmsReqDto(phone))
        if (resp.code != 0) throw RuntimeException(resp.message ?: "send sms failed")
        // Server expects dev client to use the fixed dev code "123456"
        SmsSimulator.FIXED_DEV_CODE
    }.mapHttpError()

    override suspend fun verifySms(phone: String, code: String): Result<LoginResponse> = runCatching {
        val resp = service.login(LoginReqDto(phone, code))
        if (resp.code != 0) throw RuntimeException(resp.message ?: "login failed")
        resp.data ?: throw RuntimeException("empty login response")
    }.mapHttpError()

    override suspend fun refresh(refreshToken: String): Result<LoginResponse> = runCatching {
        val resp = service.refresh(RefreshReqDto(refreshToken))
        if (resp.code != 0) throw RuntimeException(resp.message ?: "refresh failed")
        resp.data ?: throw RuntimeException("empty refresh response")
    }.mapHttpError()

    override suspend fun verifyIdCard(name: String, idCard: String): Result<Boolean> = runCatching {
        val resp = service.verifyIdCard(IdCardReqDto(name, idCard))
        if (resp.code != 0) throw RuntimeException(resp.message ?: "verify failed")
        resp.data?.verified ?: false
    }.mapHttpError()

    override suspend fun bindEhsCard(userId: String, cardNo: String): Result<String> = runCatching {
        val resp = service.bindEhs(BindEhsReqDto(cardNo))
        if (resp.code != 0) throw RuntimeException(resp.message ?: "bind failed")
        resp.data?.ehsCardNo ?: cardNo
    }.mapHttpError()

    override suspend fun bindMedicalInsurance(cardNo: String): Result<MedicalInsuranceDto> = runCatching {
        val resp = service.bindMedicalInsurance(BindMiReqDto(cardNo))
        if (resp.code != 0) throw RuntimeException(resp.message ?: "bind-mi failed")
        resp.data ?: throw RuntimeException("empty bind-mi response")
    }.mapHttpError()

    override suspend fun logout(): Result<Unit> = runCatching {
        val resp = service.logout()
        if (resp.code != 0) throw RuntimeException(resp.message ?: "logout failed")
    }.mapHttpError()

    override suspend fun me(): Result<MeResponse> = runCatching {
        val resp = service.me()
        if (resp.code != 0) throw RuntimeException(resp.message ?: "me failed")
        resp.data ?: throw RuntimeException("empty me response")
    }.mapHttpError()

    private fun <T> Result<T>.mapHttpError(): Result<T> = recoverCatching { t ->
        throw when (t) {
            is HttpException -> RuntimeException("HTTP ${t.code()}: ${t.message()}")
            else -> t
        }
    }
}