package com.example.neusoft_hospital.core.network

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class ApiError(val message: String, val code: Int = -1) : ApiResult<Nothing>()
    data object Loading : ApiResult<Nothing>()

    val isSuccess get() = this is Success
    val isError get() = this is ApiError
    val isLoading get() = this is Loading

    fun getOrNull() = when (this) { is Success -> data; else -> null }
    fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(data))
        is ApiError -> this
        is Loading -> Loading
    }
}

suspend fun <T> safeApiCall(call: suspend () -> retrofit2.Response<T>): ApiResult<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(response.body() as T)
        } else {
            ApiResult.ApiError(response.message(), response.code())
        }
    } catch (e: Throwable) {
        ApiResult.ApiError(e.message ?: "Network error")
    }
}