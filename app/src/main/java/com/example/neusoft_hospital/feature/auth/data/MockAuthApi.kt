package com.example.neusoft_hospital.feature.auth.data

import com.example.neusoft_hospital.core.network.ApiEnvelope
import com.example.neusoft_hospital.core.network.ApiProvider
import com.example.neusoft_hospital.core.util.SmsSimulator
import kotlinx.coroutines.delay
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock implementation of [AuthApiService] used when [ApiProvider.useMock] is true.
 * Keeps the exact same contract as the Retrofit-backed implementation so the
 * repository layer is agnostic of the data source.
 */
@Singleton
class MockAuthApi @Inject constructor() : AuthApiService {

    override suspend fun sendSms(phone: String): Result<String> {
        delay(ApiProvider.MOCK_DELAY_MS)
        val code = SmsSimulator.generateCode(phone)
        return Result.success(code)
    }

    override suspend fun verifySms(phone: String, code: String): Result<LoginResponse> {
        delay(ApiProvider.MOCK_DELAY_MS)
        val ok = SmsSimulator.verify(phone, code)
        return if (ok) Result.success(LoginResponse(
            token = UUID.randomUUID().toString(),
            refreshToken = UUID.randomUUID().toString(),
            userId = UUID.randomUUID().toString(),
            name = "用户${phone.takeLast(4)}",
            phone = phone,
            isVerified = false,
            hasEhsCard = false
        )) else Result.failure(Exception("验证码错误"))
    }

    override suspend fun refresh(refreshToken: String): Result<LoginResponse> {
        delay(ApiProvider.MOCK_DELAY_MS)
        return Result.success(LoginResponse(
            token = UUID.randomUUID().toString(),
            refreshToken = UUID.randomUUID().toString(),
            userId = UUID.randomUUID().toString(),
            name = "用户_mock",
            phone = "13800000000",
            isVerified = false,
            hasEhsCard = false
        ))
    }

    override suspend fun verifyIdCard(name: String, idCard: String): Result<Boolean> {
        delay(ApiProvider.MOCK_DELAY_MS)
        return Result.success(true)
    }

    override suspend fun bindEhsCard(userId: String, cardNo: String): Result<String> {
        delay(ApiProvider.MOCK_DELAY_MS)
        return Result.success("EHS${cardNo.takeLast(8)}")
    }

    override suspend fun bindMedicalInsurance(cardNo: String): Result<MedicalInsuranceDto> {
        delay(ApiProvider.MOCK_DELAY_MS)
        return Result.success(MedicalInsuranceDto(medicalInsuranceNo = cardNo, boundAt = System.currentTimeMillis()))
    }

    override suspend fun logout(): Result<Unit> = Result.success(Unit)

    override suspend fun me(): Result<MeResponse> {
        delay(ApiProvider.MOCK_DELAY_MS)
        return Result.success(MeResponse(
            userId = UUID.randomUUID().toString(),
            phone = "13800000000",
            name = "张三",
            isVerified = true,
            hasEhsCard = false
        ))
    }
}
