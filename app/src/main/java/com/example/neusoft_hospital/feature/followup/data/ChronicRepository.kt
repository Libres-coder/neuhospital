package com.example.neusoft_hospital.feature.followup.data

import com.example.neusoft_hospital.feature.auth.domain.ChronicAlert
import com.example.neusoft_hospital.feature.auth.domain.ChronicRecord
import com.example.neusoft_hospital.feature.auth.domain.ChronicType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Chronic disease repository. The server runs the threshold analysis (see
 * [com.neusoft.hospital.module.followup.FollowUpService.computeAlertLevel]).
 * Records are exposed via StateFlow so existing Flow-based UI keeps working.
 */
@Singleton
class ChronicRepository @Inject constructor(
    private val api: ChronicApiServiceRetrofit
) {
    private val recordsByType = mutableMapOf<ChronicType, MutableStateFlow<List<ChronicRecord>>>()
    private val _alerts = MutableStateFlow<List<ChronicAlert>>(emptyList())

    fun observeRecords(type: ChronicType): Flow<List<ChronicRecord>> {
        val flow = recordsByType.getOrPut(type) { MutableStateFlow(emptyList()) }
        // Fire-and-forget refresh; safe because StateFlow.value is idempotent.
        kotlinx.coroutines.MainScope().launch { refreshRecords(type) }
        return flow.asStateFlow()
    }

    fun observeAlerts(): Flow<List<ChronicAlert>> {
        kotlinx.coroutines.MainScope().launch { refreshAlerts() }
        return _alerts.asStateFlow()
    }

    suspend fun refreshRecords(type: ChronicType): Result<Unit> = runCatching {
        val resp = api.listRecords(type.name)
        if (resp.code != 0) error(resp.message ?: "load records failed")
        recordsByType[type]?.value = resp.data.orEmpty().map { it.toDomain(type) }
    }

    suspend fun refreshAlerts(): Result<Unit> = runCatching {
        val resp = api.alerts()
        if (resp.code != 0) error(resp.message ?: "load alerts failed")
        _alerts.value = resp.data.orEmpty().map { it.toDomain() }
    }

    suspend fun submit(record: ChronicRecord): Result<ChronicRecord> = runCatching {
        val resp = api.submit(SubmitChronicReqDto(
            type = record.type.name,
            date = record.date,
            systolic = record.systolic,
            diastolic = record.diastolic,
            heartRate = record.heartRate,
            fastingGlucose = record.fastingGlucose?.toDouble(),
            postprandialGlucose = record.postprandialGlucose?.toDouble(),
            hba1c = record.hba1c?.toDouble(),
            note = record.note
        ))
        if (resp.code != 0) error(resp.message ?: "submit failed")
        val saved = resp.data ?: error("empty response")
        saved.toDomain(record.type)
    }.onSuccess { refreshRecords(record.type); refreshAlerts() }

    suspend fun ackAlert(id: String) {
        runCatching {
            val resp = api.ack(id)
            if (resp.code != 0) error(resp.message ?: "ack failed")
        }
        refreshAlerts()
    }

    private fun ChronicRecordRespDto.toDomain(t: ChronicType) = ChronicRecord(
        id = id, patientId = "", type = t, date = date,
        systolic = systolic, diastolic = diastolic, heartRate = heartRate,
        fastingGlucose = fastingGlucose?.toFloat(),
        postprandialGlucose = postprandialGlucose?.toFloat(),
        hba1c = hba1c?.toFloat(),
        note = note, alertLevel = alertLevel
    )

    private fun ChronicAlertRespDto.toDomain() = ChronicAlert(
        id = id, recordId = recordId,
        type = runCatching { ChronicType.valueOf(type) }.getOrDefault(ChronicType.HYPERTENSION),
        level = level, message = message, createTime = createTime
    )
}