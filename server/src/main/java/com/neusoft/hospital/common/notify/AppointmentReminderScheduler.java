package com.neusoft.hospital.common.notify;

import com.neusoft.hospital.common.notify.SmsSendResult;
import com.neusoft.hospital.common.notify.SmsSender;
import com.neusoft.hospital.module.appointment.Appointment;
import com.neusoft.hospital.module.appointment.AppointmentRepository;
import com.neusoft.hospital.module.auth.User;
import com.neusoft.hospital.module.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Scans the appointments table once per minute and SMS-sends reminders for
 * any row that:
 * <ul>
 *   <li>has {@code reminder_set = true}</li>
 *   <li>is in an active state ({@code payed} or {@code confirmed})</li>
 *   <li>has never been reminded ({@code reminder_sent_at IS NULL})</li>
 *   <li>starts within the next ~{@code app.reminder.lead-minutes} minutes</li>
 * </ul>
 *
 * <p>Concurrency: only one instance is expected to run this in dev, so we
 * simply SELECT + UPDATE in one transaction. For multi-instance deploys,
 * swap this for a SELECT ... FOR UPDATE SKIP LOCKED or a job queue.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final SmsSender smsSender;

    @Value("${app.reminder.lead-minutes:30}")
    private int leadMinutes;

    @Value("${app.reminder.template-code:APP_REMINDER}")
    private String templateCode;

    @Value("${app.reminder.zone:Asia/Shanghai}")
    private String zoneId;

    @Value("${app.reminder.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${app.reminder.scan-interval-ms:60000}", initialDelay = 15000)
    public void scan() {
        if (!enabled) return;

        ZoneId zone = ZoneId.of(zoneId);
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime upper = now.plusMinutes(leadMinutes);
        String today = now.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String todayUpper = upper.format(DateTimeFormatter.ISO_LOCAL_DATE);

        try {
            int sent = 0;
            sent += processDay(today, now.toLocalTime().toString(), "23:59", zone);
            if (!today.equals(todayUpper)) {
                sent += processDay(todayUpper, "00:00", upper.toLocalTime().toString(), zone);
            }
            if (sent > 0) log.info("AppointmentReminderScheduler sent {} reminders", sent);
        } catch (Exception e) {
            log.warn("Reminder scan failed: {}", e.toString());
        }
    }

    private int processDay(String date, String minStart, String maxStart, ZoneId zone) {
        return appointmentRepository.findDueReminders(date, minStart, maxStart).stream()
                .mapToInt(a -> maybeRemind(a, zone))
                .sum();
    }

    @Transactional
    public int maybeRemind(Appointment a, ZoneId zone) {
        if (!a.isReminderSet()) return 0;
        if (a.getReminderSentAt() != null) return 0;
        if (!("payed".equals(a.getStatus()) || "confirmed".equals(a.getStatus()))) return 0;

        User owner = userRepository.findById(a.getPatientId()).orElse(null);
        if (owner == null || owner.getPhone() == null || owner.getPhone().isBlank()) {
            log.warn("Reminder skipped: appointment={} has no resolvable phone (patientId={})",
                    a.getId(), a.getPatientId());
            return 0;
        }

        SmsSendResult r = smsSender.send(
                owner.getPhone(),
                templateCode,
                a.getPatientName(),
                a.getDepartmentName(),
                a.getDoctorName(),
                a.getAppointmentDate(),
                a.getTimeSlot()
        );
        if (!r.isAccepted()) {
            log.warn("Reminder SMS not accepted for appointment={} err={}", a.getId(), r.getError());
            return 0;
        }
        a.setReminderSentAt(System.currentTimeMillis());
        a.setUpdateTime(System.currentTimeMillis());
        appointmentRepository.save(a);
        log.info("Reminder sent appointment={} phone={} requestId={}", a.getId(), owner.getPhone(), r.getGatewayRequestId());
        return 1;
    }
}
