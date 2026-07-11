package com.example.neusoft_hospital.core.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val ds = ctx.dataStore

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val PHONE = stringPreferencesKey("phone")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val IS_VERIFIED = booleanPreferencesKey("is_verified")
        val HAS_EHS = booleanPreferencesKey("has_ehs")
        val CURRENT_PATIENT_ID = stringPreferencesKey("current_patient_id")
    }

    val tokenFlow: Flow<String> = ds.data.map { it[Keys.TOKEN] ?: "" }
    val refreshTokenFlow: Flow<String> = ds.data.map { it[Keys.REFRESH_TOKEN] ?: "" }
    val userIdFlow: Flow<String> = ds.data.map { it[Keys.USER_ID] ?: "" }
    val isLoggedInFlow: Flow<Boolean> = ds.data.map { it[Keys.IS_LOGGED_IN] ?: false }
    val isVerifiedFlow: Flow<Boolean> = ds.data.map { it[Keys.IS_VERIFIED] ?: false }
    val hasEhsFlow: Flow<Boolean> = ds.data.map { it[Keys.HAS_EHS] ?: false }
    val currentPatientIdFlow: Flow<String> = ds.data.map { it[Keys.CURRENT_PATIENT_ID] ?: "" }
    val userNameFlow: Flow<String> = ds.data.map { it[Keys.USER_NAME] ?: "" }
    val phoneFlow: Flow<String> = ds.data.map { it[Keys.PHONE] ?: "" }

    suspend fun saveLogin(token: String, refreshToken: String, userId: String, name: String, phone: String) {
        ds.edit {
            it[Keys.TOKEN] = token
            it[Keys.REFRESH_TOKEN] = refreshToken
            it[Keys.USER_ID] = userId
            it[Keys.USER_NAME] = name
            it[Keys.PHONE] = phone
            it[Keys.IS_LOGGED_IN] = true
        }
    }

    /** Replace the access+refresh pair after a successful /refresh. */
    suspend fun updateTokens(token: String, refreshToken: String) {
        ds.edit {
            it[Keys.TOKEN] = token
            it[Keys.REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun setVerified() {
        ds.edit { it[Keys.IS_VERIFIED] = true }
    }

    suspend fun setEhsBound() {
        ds.edit { it[Keys.HAS_EHS] = true }
    }

    suspend fun setCurrentPatient(patientId: String) {
        ds.edit { it[Keys.CURRENT_PATIENT_ID] = patientId }
    }

    suspend fun logout() {
        ds.edit { it.clear() }
    }
}
