package com.example.neusoft_hospital.core.network

import android.util.Log
import com.example.neusoft_hospital.core.data.prefs.UserPreferences
import com.example.neusoft_hospital.feature.auth.data.RefreshReqDto
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Adds the Bearer access token to every outgoing request that has a stored
 * token. Login + refresh themselves do not need one, but the existing flow
 * handles that fine (the access token is short-lived and gets refreshed on
 * the first 401 anyway).
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val prefs: UserPreferences
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { prefs.tokenFlow.first() }
        val request = if (token.isNotEmpty()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}

/**
 * Handles 401 responses by exchanging the refresh token for a new
 * (access, refresh) pair via [RefreshApi], then retrying the original
 * request once. The refresh call goes through a Retrofit client that does
 * NOT include the [AuthInterceptor], so it does not recurse.
 *
 * Concurrency: a single [Mutex] serializes refresh attempts so that N
 * parallel 401s don't each spend the single-use refresh token.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val prefs: UserPreferences,
    private val refreshApi: Provider<RefreshApi>
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Don't loop forever.
        if (responseCount(response) >= 2) return null
        // Don't try to refresh a request that has no body to retry.
        if (response.request.body == null && response.request.method == "GET") {
            // ok, GETs are safe to retry
        }

        return runBlocking {
            mutex.withLock {
                val refreshToken = prefs.refreshTokenFlow.first()
                if (refreshToken.isEmpty()) return@withLock null

                val currentAccess = prefs.tokenFlow.first()
                val sentAuth = response.request.header("Authorization")
                // Another parallel 401 already refreshed while we were waiting on the mutex.
                if (sentAuth != null && sentAuth != "Bearer $currentAccess" && currentAccess.isNotEmpty()) {
                    return@withLock response.request.newBuilder()
                        .header("Authorization", "Bearer $currentAccess")
                        .build()
                }

                val refreshed = try {
                    refreshApi.get().refresh(RefreshReqDto(refreshToken))
                } catch (t: Throwable) {
                    Log.w(TAG, "refresh call threw: ${t.message}")
                    null
                } ?: return@withLock null

                if (refreshed.code != 0 || refreshed.data == null) {
                    Log.w(TAG, "refresh non-ok code=${refreshed.code} msg=${refreshed.message}")
                    return@withLock null
                }

                val newAccess = refreshed.data.token
                val newRefresh = refreshed.data.refreshToken ?: refreshToken
                prefs.updateTokens(newAccess, newRefresh)

                response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccess")
                    .build()
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var r: Response? = response
        var count = 1
        while (r?.priorResponse != null) {
            count++
            r = r.priorResponse
        }
        return count
    }

    companion object {
        private const val TAG = "TokenAuthenticator"
    }
}

/**
 * Retrofit interface used ONLY for the refresh call. Wired in [com.example.neusoft_hospital.core.di.NetworkModule]
 * with an OkHttpClient that omits [AuthInterceptor], preventing recursion.
 */
interface RefreshApi {
    @retrofit2.http.POST("api/auth/refresh")
    suspend fun refresh(@retrofit2.http.Body req: com.example.neusoft_hospital.feature.auth.data.RefreshReqDto): ApiEnvelope<RefreshBody>
}

@JsonClass(generateAdapter = true)
data class RefreshBody(
    val token: String,
    val refreshToken: String? = null
)