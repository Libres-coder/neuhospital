package com.example.neusoft_hospital.core.network

import com.squareup.moshi.JsonClass

/**
 * Wire-level envelope that mirrors the server's Result<T>.
 * All endpoints return this shape; code == 0 means success.
 */
@JsonClass(generateAdapter = true)
data class ApiEnvelope<T>(
    val code: Int,
    val message: String? = null,
    val data: T? = null
)