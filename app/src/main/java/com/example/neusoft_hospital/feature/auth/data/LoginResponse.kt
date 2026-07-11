package com.example.neusoft_hospital.feature.auth.data

import com.squareup.moshi.JsonClass

/**
 * Canonical login response used everywhere (both Mock and Retrofit).
 * Lives outside [MockAuthApi] so Retrofit DTOs and the data layer
 * share the exact same shape.
 */
@JsonClass(generateAdapter = true)
data class LoginResponse(
    val token: String,
    val refreshToken: String? = null,
    val userId: String,
    val name: String,
    val phone: String,
    val isVerified: Boolean,
    val hasEhsCard: Boolean = false
)