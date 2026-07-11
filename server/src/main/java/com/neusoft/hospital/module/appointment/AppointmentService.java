package com.neusoft.hospital.module.appointment;

import com.neusoft.hospital.common.BizException;
import com.neusoft.hospital.core.util.DateExt;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    /** Slot capacity for morning/afternoon blocks. Production: read from schedule_templates. */
    private static final int MORNING_CAPACITY = 10;
    private static final int AFTERNOON_CAPACITY = 8;

    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    // ---------- departments ----------

    public List<AppointmentDtos.DepartmentDto> listDepartments(String parentIdOrNull) {
        List<Department> rows = parentIdOrNull == null
                ? departmentRepository.findByParentIdIsNull()
                : departmentRepository.findByParentId(parentIdOrNull);
        return rows.stream().map(this::toDepartmentDto).collect(Collectors.toList());
    }

    public List<AppointmentDtos.DepartmentDto> listAllDepartments() {
        return departmentRepository.findAll().stream().map(this::toDepartmentDto).collect(Collectors.toList());
    }

    // ---------- doctors ----------

    public List<AppointmentDtos.DoctorDto> listDoctors(String departmentId) {
        return doctorRepository.findByDepartmentId(departmentId).stream()
                .map(this::toDoctorLite)
                .collect(Collectors.toList());
    }

    public AppointmentDtos.DoctorDto getDoctor(String doctorId) {
        Doctor d = doctorRepository.findById(doctorId)
                .orElseThrow(() -> BizException.notFound("Doctor not found"));
        AppointmentDtos.DoctorDto dto = toDoctorLite(d);
        dto.setSchedule(buildSchedules(doctorId));
        return dto;
    }

    /**
     * Builds the next 7 days of slot availability using ONE indexed SQL call per
     * day (was: N calls of `findAll().stream().count()` per day ? full-table scan).
     */
    private List<AppointmentDtos.ScheduleDto> buildSchedules(String doctorId) {
        List<AppointmentDtos.ScheduleDto> out = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            String date = DateExt.addDays(DateExt.today(), i);
            String dow = DateExt.weekDay(date);

            Map<String, Long> occupancy = appointmentRepository
                    .occupancyByDate(doctorId, date)
                    .stream()
                    .collect(Collectors.toMap(AppointmentRepository.SlotOccupancy::getSlot,
                                              AppointmentRepository.SlotOccupancy::getCnt));

            List<AppointmentDtos.TimeSlotDto> slots = new ArrayList<>(28);
            for (int j = 0; j < 16; j++) {
                slots.add(buildSlot(doctorId, date, 8 * 60 + j * 15, MORNING_CAPACITY, occupancy));
            }
            for (int j = 0; j < 12; j++) {
                slots.add(buildSlot(doctorId, date, 14 * 60 + j * 15, AFTERNOON_CAPACITY, occupancy));
            }
            out.add(new AppointmentDtos.ScheduleDto(date, dow, slots));
        }
        return out;
    }

    private AppointmentDtos.TimeSlotDto buildSlot(String doctorId, String date,
                                                  int start, int total,
                                                  Map<String, Long> occupancy) {
        int end = start + 15;
        String startStr = String.format("%02d:%02d", start / 60, start % 60);
        String endStr = String.format("%02d:%02d", end / 60, end % 60);
        String id = "s_" + startStr.replace(":", "") + "_" + date.replace("-", "");
        String timeRange = startStr + "-" + endStr;
        long booked = occupancy.getOrDefault(timeRange, 0L);
        int available = Math.max(0, total - (int) booked);
        return new AppointmentDtos.TimeSlotDto(id, startStr, endStr, available, total);
    }

    // ---------- appointments (CRUD) ----------

    /**
     * Book a slot. Uses:
     *  - indexed `countActiveOnSlot` to check remaining capacity
     *  - re-check inside the transaction (TOCTOU-safe)
     *  - optimistic lock via @Version
     *  - explicit rollback if concurrent writes collide
     *
     * The contract: caller MUST send an Idempotency-Key (handled upstream
     * by IdempotencyAspect ? Phase 2). Without it, retries may double-book.
     */
    @Transactional
    public AppointmentDtos.AppointmentDto book(AppointmentDtos.BookRequest req) {
        Doctor d = doctorRepository.findById(req.getDoctorId())
                .orElseThrow(() -> BizException.notFound("Doctor not found"));

        // Slot id from client ("s_0800_20260711") is converted to canonical
        // "08:00-08:15" so list queries (which key on the time-range) match it.
        String slotId = req.getTimeSlot();
        String timeRange = slotIdToTimeRange(slotId);

        // Capacity check (indexed via (doctor_id, appointment_date)).
        long booked = appointmentRepository.countActiveOnSlot(
                d.getId(), req.getDate(), timeRange);
        if (booked >= pickCapacityForSlot(timeRange)) {
            throw BizException.badRequest("Slot is full");
        }

        long now = System.currentTimeMillis();
        Appointment a = new Appointment();
        a.setId("ap_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        a.setPatientId(req.getPatientId());
        a.setPatientName(req.getPatientName());
        a.setDoctorId(d.getId());
        a.setDoctorName(d.getName());
        a.setDepartmentId(d.getDepartmentId());
        a.setDepartmentName(d.getDepartmentName());
        a.setAppointmentDate(req.getDate());
        a.setTimeSlot(timeRange);
        a.setDuration(req.getDuration());
        a.setStatus("payed");
        a.setReminderSet(false);
        a.setCreateTime(now);
        a.setUpdateTime(now);

        try {
            appointmentRepository.saveAndFlush(a);
        } catch (OptimisticLockingFailureException e) {
            throw BizException.conflict("Slot just got booked by someone else, please retry");
        } catch (DataIntegrityViolationException e) {
            // Surface the real reason (NOT NULL, FK, duplicate, length ?) instead
            // of masking every DB error as "Slot is full".
            throw BizException.badRequest("Booking rejected: " + rootCauseMessage(e));
        }
        return toAppointmentDto(a);
    }

    /**
     * Map "s_0800_20260711" -> "08:00-08:15".
     * If the input is already in time-range form, return it as-is.
     */
    static String slotIdToTimeRange(String slotOrRange) {
        if (slotOrRange == null) return null;
        if (!slotOrRange.startsWith("s_")) return slotOrRange;
        String[] parts = slotOrRange.split("_");
        // parts: ["s", "HHMM", "YYYYMMDD"]
        if (parts.length < 2 || parts[1].length() != 4) return slotOrRange;
        String hhmm = parts[1];
        String start = hhmm.substring(0, 2) + ":" + hhmm.substring(2, 4);
        int endMin = Integer.parseInt(hhmm.substring(2, 4)) + 15;
        int hour = Integer.parseInt(hhmm.substring(0, 2)) + (endMin / 60);
        int minute = endMin % 60;
        String end = String.format("%02d:%02d", hour, minute);
        return start + "-" + end;
    }

    private static String rootCauseMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur.getMessage() == null ? t.getClass().getSimpleName() : cur.getMessage();
    }

    /**
     * Capacity for a given slot.
     * Accepts either the canonical time-range "08:00-08:15" (DB format) or the
     * client-facing slot id "s_0800_20260711" used by /api/doctors/{id}.
     */
    private int pickCapacityForSlot(String timeRange) {
        int startHour;
        if (timeRange != null && timeRange.startsWith("s_")) {
            // "s_0800_20260711" -> "0800"
            String hhmm = timeRange.split("_")[1];
            startHour = Integer.parseInt(hhmm.substring(0, 2));
        } else {
            // "08:00-08:15"
            startHour = Integer.parseInt(timeRange.substring(0, 2));
        }
        return startHour < 12 ? MORNING_CAPACITY : AFTERNOON_CAPACITY;
    }

    @Transactional
    public void cancel(String appointmentId, String userId) {
        Appointment a = ownedOrThrow(appointmentId, userId);
        if ("cancelled".equals(a.getStatus())) return;
        if ("completed".equals(a.getStatus())) {
            throw BizException.badRequest("Cannot cancel a completed appointment");
        }
        a.setStatus("cancelled");
        a.setUpdateTime(System.currentTimeMillis());
        try {
            appointmentRepository.saveAndFlush(a);
        } catch (OptimisticLockingFailureException e) {
            throw BizException.conflict("Record was modified by another request, please retry");
        }
    }

    @Transactional
    public void pay(String appointmentId, String userId) {
        Appointment a = ownedOrThrow(appointmentId, userId);
        a.setStatus("payed");
        a.setUpdateTime(System.currentTimeMillis());
        try {
            appointmentRepository.saveAndFlush(a);
        } catch (OptimisticLockingFailureException e) {
            throw BizException.conflict("Record was modified by another request, please retry");
        }
    }

    @Transactional
    public void noShow(String appointmentId, String userId) {
        Appointment a = ownedOrThrow(appointmentId, userId);
        a.setStatus("no_show");
        a.setUpdateTime(System.currentTimeMillis());
        try {
            appointmentRepository.saveAndFlush(a);
        } catch (OptimisticLockingFailureException e) {
            throw BizException.conflict("Record was modified by another request, please retry");
        }
    }

    @Transactional
    public void setReminder(String appointmentId, String userId, boolean on) {
        Appointment a = ownedOrThrow(appointmentId, userId);
        a.setReminderSet(on);
        a.setUpdateTime(System.currentTimeMillis());
        try {
            appointmentRepository.saveAndFlush(a);
        } catch (OptimisticLockingFailureException e) {
            throw BizException.conflict("Record was modified by another request, please retry");
        }
    }

    public List<AppointmentDtos.AppointmentDto> myAppointments(String userId) {
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(userId).stream()
                .map(this::toAppointmentDto)
                .collect(Collectors.toList());
    }

    public int noShowCount(String userId) {
        return appointmentRepository.countNoShow(userId);
    }

    // ---------- recommend (history + specialty aware) ----------

    /**
     * Book the best-matching doctor at the earliest available slot based on
     * the user's symptoms and history. One call replaces the manual pick
     * (recommended in triage ? DoctorList ? pick doctor ? pick slot).
     *
     * <p>Returns null if no doctor can be recommended (empty recommendation
     * list) so the caller can show an explicit "???????" UI.</p>
     */
    @Transactional
    public AppointmentDtos.AppointmentDto bookFromTriage(String userId, String patientName, String symptoms) {
        List<DoctorRecommendDto> ranked = recommend(userId, symptoms);
        for (DoctorRecommendDto r : ranked) {
            if (r.nextAvailableDate == null || r.nextAvailableSlot == null) continue;
            AppointmentDtos.BookRequest req = new AppointmentDtos.BookRequest();
            req.setDoctorId(r.doctor.getId());
            req.setDate(r.nextAvailableDate);
            req.setTimeSlot(r.nextAvailableSlot);
            req.setDuration(15);
            req.setPatientId(userId);
            req.setPatientName(patientName);
            try {
                return book(req);
            } catch (BizException e) {
                // slot may have just been booked by someone else; try the next recommendation
                if (e.getCode() == 400 || e.getCode() == 409) continue;
                throw e;
            }
        }
        return null;
    }

    /**
     * Rank doctors for a user based on:
     *   1. <strong>specialty match</strong> ? token overlap between symptoms and
     *      {@code doctor.expertise} (delimiters: ,?;?|/)
     *   2. <strong>user history</strong> ? recent (?6 months) appointments in
     *      the same department boost the score. Frequency ? recency weight.
     *   3. <strong>rating</strong> ? normalized to [0,1]
     *   4. <strong>availability</strong> ? doctors with no remaining slots
     *      today are deprioritized (not removed: user might still want to
     *      book for tomorrow).
     *
     * <p>Returns top 5 with the earliest available slot pre-computed so the
     * Android UI can render "next bookable time" without a second round-trip.</p>
     */
    public List<DoctorRecommendDto> recommend(String userId, String symptoms) {
        String symText = symptoms == null ? "" : symptoms.trim();
        Set<String> symTokens = tokenize(symText);

        // 1. User's recent department usage (count per department).
        Map<String, Long> historyCounts = userId == null
                ? Map.of()
                : appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(userId).stream()
                        .filter(a -> !"cancelled".equals(a.getStatus()))
                        .filter(a -> a.getCreateTime() >= System.currentTimeMillis() - 180L * 24 * 3600 * 1000)
                        .collect(Collectors.groupingBy(Appointment::getDepartmentId, Collectors.counting()));

        long historyMax = historyCounts.values().stream().mapToLong(Long::longValue).max().orElse(0L);

        String today = DateExt.today();

        return doctorRepository.findAll().stream().map(d -> {
            // (1) specialty match ? Jaccard-like overlap on tokens
            Set<String> expertiseTokens = tokenize(d.getExpertise());
            double specialtyScore = jaccard(symTokens, expertiseTokens);

            // (2) history boost ? relative to max count (so a user with many
            //     visits to one dept sees that dept dominate)
            long h = historyCounts.getOrDefault(d.getDepartmentId(), 0L);
            double historyScore = historyMax > 0 ? (double) h / historyMax : 0.0;

            // (3) rating
            double rating = d.getRating() == null ? 4.5 : d.getRating();
            double ratingScore = rating / 5.0;

            // (4) availability (today)
            Map<String, Long> occ = appointmentRepository
                    .occupancyByDate(d.getId(), today)
                    .stream()
                    .collect(Collectors.toMap(AppointmentRepository.SlotOccupancy::getSlot,
                                              AppointmentRepository.SlotOccupancy::getCnt));
            boolean hasToday = firstAvailableTime(d.getId(), today, occ).isPresent();

            // Weighted combination: specialty > history > availability > rating
            double score = specialtyScore * 0.50
                    + historyScore * 0.20
                    + (hasToday ? 0.15 : 0.05)
                    + ratingScore * 0.15;

            DoctorRecommendDto rec = new DoctorRecommendDto(toDoctorLite(d), (float) score);
            rec.historyCount = h;
            rec.specialtyMatched = expertiseTokens.stream().anyMatch(symTokens::contains);
            rec.nextAvailableDate = null;
            rec.nextAvailableSlot = null;
            findFirstAvailable(d.getId(), today, 3).ifPresent(s -> {
                rec.nextAvailableDate = s.date();
                rec.nextAvailableSlot = s.slot();
            });
            return rec;
        })
        .sorted((a, b) -> Float.compare(b.score, a.score))
        .limit(5)
        .collect(Collectors.toList());
    }

    /** Tokenize Chinese / English mixed text by punctuation and whitespace. */
    private static Set<String> tokenize(String s) {
        if (s == null || s.isBlank()) return Set.of();
        String[] parts = s.split("[,?;?/?|\\s?]+");
        Set<String> out = new HashSet<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0.0 : (double) inter.size() / union.size();
    }

    /** Find earliest available slot across the next {@code daysAhead} days. */
    private Optional<SlotPick> findFirstAvailable(String doctorId, String startDate, int daysAhead) {
        for (int i = 0; i < daysAhead; i++) {
            String date = DateExt.addDays(startDate, i);
            Map<String, Long> occ = appointmentRepository
                    .occupancyByDate(doctorId, date)
                    .stream()
                    .collect(Collectors.toMap(AppointmentRepository.SlotOccupancy::getSlot,
                                              AppointmentRepository.SlotOccupancy::getCnt));
            Optional<String> first = firstAvailableTime(doctorId, date, occ);
            if (first.isPresent()) return Optional.of(new SlotPick(date, first.get()));
        }
        return Optional.empty();
    }

    private Optional<String> firstAvailableTime(String doctorId, String date, Map<String, Long> occupancy) {
        for (int j = 0; j < 16; j++) {
            String slot = pickFirstSlot(8 * 60 + j * 15, MORNING_CAPACITY, occupancy);
            if (slot != null) return Optional.of(slot);
        }
        for (int j = 0; j < 12; j++) {
            String slot = pickFirstSlot(14 * 60 + j * 15, AFTERNOON_CAPACITY, occupancy);
            if (slot != null) return Optional.of(slot);
        }
        return Optional.empty();
    }

    private static String pickFirstSlot(int startMin, int total, Map<String, Long> occupancy) {
        String startStr = String.format("%02d:%02d", startMin / 60, startMin % 60);
        String endStr   = String.format("%02d:%02d", (startMin + 15) / 60, (startMin + 15) % 60);
        String timeRange = startStr + "-" + endStr;
        long booked = occupancy.getOrDefault(timeRange, 0L);
        if (booked >= total) return null;
        return timeRange;
    }

    private record SlotPick(String date, String slot) {}

    // ---------- helpers ----------

    private Appointment ownedOrThrow(String id, String userId) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("Appointment not found"));
        if (!a.getPatientId().equals(userId)) {
            throw BizException.forbidden("Cannot modify another user's appointment");
        }
        return a;
    }

    private AppointmentDtos.DepartmentDto toDepartmentDto(Department d) {
        return new AppointmentDtos.DepartmentDto(
                d.getId(), d.getParentId(), d.getName(), d.getNamePy(), d.getDescription(), d.getIconUrl());
    }

    private AppointmentDtos.DoctorDto toDoctorLite(Doctor d) {
        double r = (d.getRating() == null ? 4.5 : d.getRating());
        return new AppointmentDtos.DoctorDto(
                d.getId(), d.getName(), d.getDepartmentId(), d.getDepartmentName(),
                d.getTitle(), d.getExpertise(), d.getProfile(),
                (float) r,
                d.getAvatarUrl(),
                List.of());
    }

    private AppointmentDtos.AppointmentDto toAppointmentDto(Appointment a) {
        return new AppointmentDtos.AppointmentDto(
                a.getId(), a.getPatientId(), a.getPatientName(),
                a.getDoctorId(), a.getDoctorName(),
                a.getDepartmentId(), a.getDepartmentName(),
                a.getAppointmentDate(), a.getTimeSlot(),
                a.getDuration(), a.getStatus(), a.isReminderSet(), a.getCreateTime());
    }

    /** Used only by [recommend] output ? score goes alongside the DTO. */
    /**
     * Recommendation row. Public-facing fields (kept public for Jackson
     * serialization).
     */
    public static final class DoctorRecommendDto {
        public final AppointmentDtos.DoctorDto doctor;
        public final float score;
        public long historyCount;
        public boolean specialtyMatched;
        public String nextAvailableDate;
        public String nextAvailableSlot;

        public DoctorRecommendDto(AppointmentDtos.DoctorDto doctor, float score) {
            this.doctor = doctor;
            this.score = score;
        }
    }

    /** Legacy alias kept for any future caller that wants the simpler shape. */
    public record DoctorDtoWithScore(AppointmentDtos.DoctorDto doctor, float score) {}
}