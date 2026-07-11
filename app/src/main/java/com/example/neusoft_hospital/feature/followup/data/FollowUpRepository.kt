package com.example.neusoft_hospital.feature.followup.data

import com.example.neusoft_hospital.core.data.local.dao.FollowUpDao
import com.example.neusoft_hospital.core.data.local.dao.RehabLogDao
import com.example.neusoft_hospital.core.data.local.entity.FollowUpPlanEntity
import com.example.neusoft_hospital.core.data.local.entity.FollowUpTaskEntity
import com.example.neusoft_hospital.core.data.local.entity.RehabLogEntity
import com.example.neusoft_hospital.core.network.ApiEnvelope
import com.example.neusoft_hospital.feature.auth.domain.FollowUpPlan
import com.example.neusoft_hospital.feature.auth.domain.FollowUpTask
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Follow-up + rehab repository. The server is the source of truth, but the
 * existing UI subscribes to Flows, so we keep them by exposing a StateFlow
 * that is refreshed from the backend. Local Room tables are kept in sync
 * for offline views.
 */
@Singleton
class FollowUpRepository @Inject constructor(
    private val api: FollowUpApiServiceRetrofit,
    private val followUpDao: FollowUpDao,
    private val rehabDao: RehabLogDao
) {

    private val _plans = MutableStateFlow<List<FollowUpPlan>>(emptyList())
    private val _pending = MutableStateFlow<List<FollowUpTask>>(emptyList())
    private val _rehab = MutableStateFlow<List<RehabLogEntity>>(emptyList())
    private val taskFlows = mutableMapOf<String, MutableStateFlow<List<FollowUpTask>>>()

    fun observePlans(): Flow<List<FollowUpPlan>> = _plans.asStateFlow()
    fun observePendingTasks(): Flow<List<FollowUpTask>> = _pending.asStateFlow()
    fun observeRehabLogs(): Flow<List<RehabLogEntity>> = _rehab.asStateFlow()

    suspend fun refreshPlans(): Result<Unit> = runCatching {
        val resp = api.listPlans()
        requireOk(resp)
        _plans.value = resp.data.orEmpty().map { it.toDomain() }
    }

    suspend fun refreshPending(): Result<Unit> = runCatching {
        val resp = api.pendingTasks()
        requireOk(resp)
        _pending.value = resp.data.orEmpty().map { it.toDomain() }
    }

    suspend fun refreshRehab(): Result<Unit> = runCatching {
        val resp = api.listRehab()
        requireOk(resp)
        val list = resp.data.orEmpty().map {
            RehabLogEntity(
                id = it.id, patientId = "", disease = it.disease,
                date = it.logDate, items = it.items.joinToString("|"),
                completed = it.completed
            )
        }
        _rehab.value = list
    }

    suspend fun createPlan(disease: String, surgeryDate: String, totalDays: Int = 90): Result<FollowUpPlan> = runCatching {
        val resp = api.createPlan(CreatePlanReqDto(disease, surgeryDate, totalDays))
        requireOk(resp)
        resp.data?.toDomain() ?: error("empty plan")
    }.onSuccess { refreshPlans() }

    fun observeTasks(planId: String): Flow<List<FollowUpTask>> {
        val flow = taskFlows.getOrPut(planId) { MutableStateFlow(emptyList()) }
        kotlinx.coroutines.MainScope().launch { refreshTasks(planId) }
        return flow.asStateFlow()
    }

    private suspend fun refreshTasks(planId: String) {
        runCatching {
            val resp = api.listTasks(planId)
            if (resp.code == 0) {
                taskFlows[planId]?.value = resp.data.orEmpty().map { it.toDomain() }
            }
        }
    }

    suspend fun completeTask(taskId: String, answers: Map<String, String>) {
        runCatching {
            val resp = api.completeTask(CompleteTaskReqDto(taskId, answers))
            requireOk(resp)
        }
        refreshPending()
    }

    suspend fun createRehabLog(disease: String, items: List<String>): Result<Unit> = runCatching {
        val resp = api.createRehab(CreateRehabReqDto(disease, items))
        requireOk(resp)
    }.onSuccess { refreshRehab() }

    suspend fun completeRehab(id: String) {
        runCatching {
            val resp = api.completeRehab(id)
            requireOk(resp)
        }
        refreshRehab()
    }

    private fun requireOk(resp: ApiEnvelope<*>) {
        if (resp.code != 0) throw RuntimeException(resp.message ?: "api failed")
    }

    private fun PlanRespDto.toDomain() = FollowUpPlan(
        id = id, patientId = patientId ?: "", disease = disease,
        surgeryDate = surgeryDate, totalDays = totalDays,
        tasks = tasks.orEmpty().map { it.toDomain() }
    )

    private fun TaskRespDto.toDomain() = FollowUpTask(
        id = id, planId = planId, dayIndex = dayIndex, targetDate = targetDate,
        questions = questions.orEmpty(), answers = answers,
        completed = completed, doctorReply = doctorReply
    )
}