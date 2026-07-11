package com.example.neusoft_hospital.feature.appointment.data

import com.example.neusoft_hospital.core.data.local.dao.AppointmentDao
import com.example.neusoft_hospital.core.data.local.entity.AppointmentEntity
import com.example.neusoft_hospital.core.network.ApiProvider
import com.example.neusoft_hospital.core.util.DateExt
import com.example.neusoft_hospital.feature.auth.domain.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline implementation of [AppointmentApiService]. Used when [ApiProvider.useMock] is true.
 * Keeps the same data shape and behaviour as the previous version so the whole app
 * still runs end-to-end without the Spring Boot backend.
 */
@Singleton
class MockAppointmentApi @Inject constructor(
    private val dao: AppointmentDao
) : AppointmentApiService {

    private val departments = listOf(
        Department("d1", null, "内科", "neike", "内科疾病诊治"),
        Department("d1_1", "d1", "心血管内科", "xinxueguanneike", "高血压、冠心病、心力衰竭"),
        Department("d1_2", "d1", "呼吸内科", "huxineike", "哮喘、慢阻肺、肺炎"),
        Department("d1_3", "d1", "消化内科", "xiaohuaneike", "胃炎、胃溃疡、肝病"),
        Department("d2", null, "外科", "waike", "外科疾病诊治"),
        Department("d2_1", "d2", "普通外科", "putongwaike", "胃肠、肝胆、甲状腺"),
        Department("d2_2", "d2", "骨科", "guke", "骨折、关节、脊柱"),
        Department("d3", null, "妇产科", "fuchanke", "妇科、产科疾病"),
        Department("d4", null, "儿科", "erke", "儿童疾病诊治"),
        Department("d5", null, "皮肤科", "pifuke", "皮肤病诊治"),
        Department("d6", null, "眼科", "yanke", "眼疾诊治"),
        Department("d7", null, "口腔科", "kouqiangk", "口腔疾病诊治"),
        Department("d8", null, "耳鼻喉科", "erbihouke", "耳鼻喉疾病诊治"),
        Department("d9", null, "中医科", "zhongyike", "中医诊治")
    )

    private val doctors = listOf(
        Doctor("doc1", "李建华", "d1_1", "心血管内科", "主任医师", "高血压、冠心病、心力衰竭", "从事心血管疾病诊治30年。", 4.8f),
        Doctor("doc2", "王玉梅", "d1_1", "心血管内科", "副主任医师", "心律失常、起搏器", "心律失常介入治疗20年。", 4.6f),
        Doctor("doc3", "张志强", "d1_2", "呼吸内科", "主任医师", "慢阻肺、哮喘", "呼吸分会委员，SCI 30余篇。", 4.7f),
        Doctor("doc4", "陈晓东", "d1_3", "消化内科", "副主任医师", "胃肠镜、肝病", "精通胃肠镜检查及治疗。", 4.5f),
        Doctor("doc5", "刘文静", "d2_1", "普通外科", "主任医师", "肝胆、胃肠肿瘤", "微创手术治疗。", 4.9f),
        Doctor("doc6", "赵明远", "d2_2", "骨科", "副主任医师", "关节、骨折", "关节置换专家。", 4.4f),
        Doctor("doc7", "孙美丽", "d3", "妇产科", "主任医师", "妇科肿瘤、产科", "30年妇产科经验。", 4.8f),
        Doctor("doc8", "周明亮", "d4", "儿科", "副主任医师", "新生儿、儿童常见病", "新生儿疾病诊治。", 4.6f),
        Doctor("doc9", "吴秀英", "d5", "皮肤科", "主治医师", "湿疹、银屑病、痤疮", "皮肤病诊疗经验丰富。", 4.5f),
        Doctor("doc10", "郑鹏程", "d6", "眼科", "副主任医师", "白内障、近视手术", "白内障超声乳化手术专家。", 4.7f),
        Doctor("doc11", "冯兰", "d7", "口腔科", "主治医师", "牙周、种植牙", "口腔综合治疗。", 4.3f),
        Doctor("doc12", "韩雪", "d8", "耳鼻喉科", "副主任医师", "中耳炎、鼻窦炎", "耳鼻喉内镜手术专家。", 4.5f),
        Doctor("doc13", "何明", "d9", "中医科", "主任医师", "中医调理、针灸", "中医世家出身。", 4.9f)
    )

    // in-memory "my appointments" mirror so the mock implementation is self-contained.
    private val myAppointments = mutableMapOf<String, AppointmentEntity>()
    private val counter = java.util.concurrent.atomic.AtomicInteger(0)

    /**
     * In-memory counter for *active in-process* bookings/cancellations. This is intentionally
     * process-local: the authoritative source for "already booked" is Room, queried fresh on
     * every [getDoctor] call. Keeping it additive (no Room resync) avoids double counting.
     */
    private val bookedCount = mutableMapOf<String, Int>()
    private fun slotKey(doctorId: String, date: String, slotId: String) = "$doctorId|$date|$slotId"

    /**
     * Returns the in-process booked count for a slot (0 if none). The caller is responsible
     * for also subtracting Room-persisted active appointments to get the true remaining capacity.
     */
    private fun bookedCountFor(doctorId: String, date: String, slotId: String): Int =
        bookedCount[slotKey(doctorId, date, slotId)] ?: 0

    /**
     * Builds the slot list for a given day, subtracting already-booked appointments from
     * the dynamically generated `available` value. Clamps to 0 so the UI never goes negative.
     *
     * [roomOccupied] is the count of active (non-cancelled, non-no-show) appointments in Room
     * for each slot id on this day, computed fresh per call. Combined with the in-process
     * [bookedCount] we get the true remaining capacity.
     */
    private fun generateSlots(
        dayOffset: Int,
        doctorId: String,
        date: String,
        roomOccupied: Map<String, Int>
    ): List<TimeSlot> {
        val buildBase: (Int, Int) -> TimeSlot = { startMin, cap ->
            val h = startMin / 60; val m = startMin % 60
            val endMin = startMin + 15
            val endH = endMin / 60; val endM = endMin % 60
            TimeSlot(
                id = "s_${h}${m}_${dayOffset}",
                startTime = String.format("%02d:%02d", h, m),
                endTime = String.format("%02d:%02d", endH, endM),
                available = 0,
                total = cap
            )
        }
        val slots = mutableListOf<TimeSlot>()
        for (i in 0 until 16) {
            val start = 8 * 60 + i * 15
            val raw = if ((dayOffset + i) % 5 == 0) 0 else (10 - (i % 7))
            val slot = buildBase(start, 10).copy(available = raw)
            slots.add(slot)
        }
        for (i in 0 until 12) {
            val start = 14 * 60 + i * 15
            val raw = if ((dayOffset + i) % 3 == 0) 0 else (8 - (i % 5))
            val slot = buildBase(start, 8).copy(available = raw)
            slots.add(slot)
        }
        return slots.map { s ->
            val memUsed = bookedCountFor(doctorId, date, s.id)
            val roomUsed = roomOccupied[s.id] ?: 0
            val used = (memUsed + roomUsed).coerceAtLeast(0)
            s.copy(available = (s.available - used).coerceAtLeast(0))
        }
    }

    /** Computes, from Room, the count of active appointments per slot id on a given date for a doctor. */
    private suspend fun roomOccupancyFor(doctorId: String, date: String): Map<String, Int> {
        val all = dao.getAll().first()
        return all.asSequence()
            .filter { it.doctorId == doctorId && it.date == date && it.status !in setOf("cancelled", "no_show") }
            .mapNotNull { a -> findSlotIdFor(a.doctorId, a.date, a.timeSlot)?.let { it to a } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, list) -> list.size }
    }

    override suspend fun getDepartments(parentId: String?): Result<List<Department>> {
        delay(ApiProvider.MOCK_DELAY_MS)
        return Result.success(departments.filter { it.parentId == parentId })
    }

    override suspend fun getAllDepartments(): Result<List<Department>> = Result.success(departments)

    override suspend fun getDoctors(departmentId: String): Result<List<Doctor>> {
        delay(ApiProvider.MOCK_DELAY_MS)
        return Result.success(doctors.filter { it.departmentId == departmentId })
    }

    override suspend fun getDoctor(doctorId: String): Result<Doctor> {
        delay(ApiProvider.MOCK_DELAY_MS)
        val d = doctors.firstOrNull { it.id == doctorId }
            ?: return Result.failure(Exception("医生不存在"))
        val schedules = (0..6).map { offset ->
            val date = DateExt.addDays(DateExt.today(), offset)
            Schedule(
                date = date,
                dayOfWeek = DateExt.weekDay(date),
                slots = generateSlots(offset, doctorId, date, roomOccupancyFor(doctorId, date))
            )
        }
        return Result.success(d.copy(schedule = schedules))
    }

    private fun findSlotIdFor(doctorId: String, date: String, timeSlot: String): String? {
        // The static `doctors` list has no schedule attached, so we reconstruct the slot id
        // by matching the start time against the same generator logic used by getDoctor().
        val parts = timeSlot.split("-")
        if (parts.size != 2) return null
        val start = parts[0] // "HH:MM"
        val h = start.substringBefore(":").toIntOrNull() ?: return null
        val m = start.substringAfter(":").toIntOrNull() ?: return null
        val dayOffset = (0..6).firstOrNull { off ->
            DateExt.addDays(DateExt.today(), off) == date
        } ?: return null
        // Slot id format used by generateSlots(): "s_${h}${m}_${dayOffset}" (no zero padding).
        return "s_${h}${m}_${dayOffset}"
    }

    override suspend fun recommend(symptoms: String, historyDept: String?): Result<List<DoctorRecommendation>> {
        delay(ApiProvider.MOCK_DELAY_MS)
        val keyword = symptoms
        val scored = doctors.map { d ->
            val expertiseScore = if (d.expertise.contains(keyword, ignoreCase = true) || d.profile.contains(keyword, ignoreCase = true)) 0.5f else 0.2f
            val historyScore = if (historyDept != null && d.departmentId == historyDept) 0.3f else 0.1f
            val ratingScore = (d.rating / 5.0).toFloat() * 0.2f
            val total = expertiseScore + historyScore + ratingScore
            DoctorRecommendation(d, total, buildList {
                if (expertiseScore > 0.3f) add("专业方向匹配")
                if (historyScore > 0.2f) add("历史好评率高")
                if (ratingScore > 0.15f) add("综合评分高")
            })
        }.sortedByDescending { it.score }
        return Result.success(scored.take(5))
    }

    override suspend fun recommendAndBook(symptoms: String): Result<AppointmentEntity> {
        delay(ApiProvider.MOCK_DELAY_MS)
        val top = doctors.maxByOrNull { it.rating }
            ?: return Result.failure(IllegalStateException("no doctors"))
        val today = java.time.LocalDate.now().toString()
        val ap = AppointmentEntity(
            id = "ap_" + java.util.UUID.randomUUID().toString().take(12),
            patientId = "self",
            patientName = "本人",
            doctorId = top.id,
            doctorName = top.name,
            departmentId = top.departmentId,
            departmentName = top.departmentName,
            date = today,
            timeSlot = "08:00-08:15",
            duration = 15,
            status = "payed",
            reminderSet = false,
            createTime = System.currentTimeMillis()
        )
        return Result.success(ap)
    }

    override suspend fun book(
        doctorId: String,
        date: String,
        timeSlot: String,
        duration: Int,
        patientId: String,
        patientName: String
    ): Result<AppointmentEntity> {
        delay(ApiProvider.MOCK_DELAY_MS)
        val doctorRes = getDoctor(doctorId)
        val doctor = doctorRes.getOrNull() ?: return Result.failure(Exception("医生不存在"))
        val slotId = timeSlot  // timeSlot passed from BookingScreen already is "<start>-<end>"; we use the doctor/date key
        // Reject duplicate bookings: same patient + same doctor + same date + same time slot string.
        // Source of truth is Room (the cache the UI reads from), so this matches what the UI shows.
        // Cancelled / no_show appointments free the slot up again.
        val all = dao.getByPatient(patientId).first()
        val duplicate = all.any {
            it.doctorId == doctorId && it.patientId == patientId && it.date == date &&
                it.timeSlot == timeSlot && it.status !in setOf("cancelled", "no_show")
        }
        if (duplicate) return Result.failure(Exception("您已预约该时段，请勿重复预约"))

        val entity = AppointmentEntity(
            id = "ap_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16),
            patientId = patientId, patientName = patientName,
            doctorId = doctorId, doctorName = doctor.name,
            departmentId = doctor.departmentId, departmentName = doctor.departmentName,
            date = date, timeSlot = timeSlot, duration = duration,
            status = "payed", createTime = System.currentTimeMillis(),
            reminderSet = false
        )
        dao.insert(entity)
        // Room is the authoritative source of occupancy. Don't touch the in-process map here
        // to avoid double counting on the next getDoctor().
        return Result.success(entity)
    }

    override suspend fun cancel(id: String): Result<Unit> {
        delay(ApiProvider.MOCK_DELAY_MS)
        val a = dao.getById(id) ?: return Result.failure(Exception("预约不存在"))
        dao.updateStatus(id, "cancelled")
        // No bookedCount update: Room is the source of truth and the next getDoctor()
        // will recompute availability from `roomOccupancyFor()`.
        return Result.success(Unit)
    }

    override suspend fun pay(id: String): Result<Unit> {
        delay(ApiProvider.MOCK_DELAY_MS)
        if (dao.getById(id) == null) return Result.failure(Exception("预约不存在"))
        dao.updateStatus(id, "payed")
        return Result.success(Unit)
    }

    override suspend fun noShow(id: String): Result<Unit> {
        delay(ApiProvider.MOCK_DELAY_MS)
        val a = dao.getById(id) ?: return Result.failure(Exception("预约不存在"))
        dao.updateStatus(id, "no_show")
        // No bookedCount update: Room is the source of truth.
        return Result.success(Unit)
    }

    override suspend fun setReminder(id: String, set: Boolean): Result<Unit> {
        delay(ApiProvider.MOCK_DELAY_MS)
        if (dao.getById(id) == null) return Result.failure(Exception("预约不存在"))
        dao.setReminder(id, set)
        return Result.success(Unit)
    }

    override suspend fun myAppointments(): Result<List<AppointmentEntity>> {
        delay(ApiProvider.MOCK_DELAY_MS)
        // Source of truth is Room — return every persisted row so refreshMyAppointments()
        // can re-upsert them after a process restart.
        val rows = dao.getAll().first()
        return Result.success(rows)
    }

    override suspend fun noShowCount(): Result<Int> {
        delay(ApiProvider.MOCK_DELAY_MS)
        val rows = dao.getAll().first()
        return Result.success(rows.count { it.status == "no_show" })
    }
}