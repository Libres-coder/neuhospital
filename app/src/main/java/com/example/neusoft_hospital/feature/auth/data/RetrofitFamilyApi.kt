package com.example.neusoft_hospital.feature.auth.data

import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitFamilyApi @Inject constructor(
    private val service: FamilyApiServiceRetrofit
) : FamilyApiService {

    override suspend fun list(): Result<FamilyListDto> = runCatching {
        val resp = service.list()
        if (resp.code != 0) throw RuntimeException(resp.message ?: "family list failed")
        resp.data ?: throw RuntimeException("empty family list")
    }.mapHttpError()

    override suspend fun add(req: FamilyAddReqDto): Result<FamilyMemberDto> = runCatching {
        val resp = service.add(req)
        if (resp.code != 0) throw RuntimeException(resp.message ?: "family add failed")
        resp.data ?: throw RuntimeException("empty add response")
    }.mapHttpError()

    override suspend fun update(id: String, req: FamilyUpdateReqDto): Result<FamilyMemberDto> = runCatching {
        val resp = service.update(id, req)
        if (resp.code != 0) throw RuntimeException(resp.message ?: "family update failed")
        resp.data ?: throw RuntimeException("empty update response")
    }.mapHttpError()

    override suspend fun delete(id: String): Result<Unit> = runCatching {
        val resp = service.delete(id)
        if (resp.code != 0) throw RuntimeException(resp.message ?: "family delete failed")
    }.mapHttpError()

    override suspend fun setDefault(id: String): Result<FamilyMemberDto> = runCatching {
        val resp = service.setDefault(id)
        if (resp.code != 0) throw RuntimeException(resp.message ?: "set-default failed")
        resp.data ?: throw RuntimeException("empty set-default response")
    }.mapHttpError()

    private fun <T> Result<T>.mapHttpError(): Result<T> = recoverCatching { t ->
        throw when (t) {
            is HttpException -> RuntimeException("HTTP ${t.code()}: ${t.message()}")
            else -> t
        }
    }
}
