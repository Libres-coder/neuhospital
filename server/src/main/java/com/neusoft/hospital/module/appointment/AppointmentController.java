package com.neusoft.hospital.module.appointment;

import com.neusoft.hospital.common.BizException;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.module.auth.User;
import com.neusoft.hospital.module.auth.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Appointment", description = "Departments / doctors / schedules / appointments")
public class AppointmentController {

    private final AppointmentService service;
    private final UserRepository userRepository;

    // ---------- public reads ----------

    @GetMapping("/departments")
    @Operation(summary = "List child departments; parentId null = top level")
    public Result<Object> listDepartments(@RequestParam(required = false) String parentId) {
        return Result.ok(service.listDepartments(parentId));
    }

    @GetMapping("/departments/all")
    @Operation(summary = "List all departments (for building trees)")
    public Result<Object> listAllDepartments() {
        return Result.ok(service.listAllDepartments());
    }

    @GetMapping("/doctors")
    @Operation(summary = "List doctors by department")
    public Result<Object> listDoctors(@RequestParam String departmentId) {
        return Result.ok(service.listDoctors(departmentId));
    }

    @GetMapping("/doctors/{id}")
    @Operation(summary = "Doctor detail with 7-day schedule")
    public Result<Object> getDoctor(@PathVariable String id) {
        return Result.ok(service.getDoctor(id));
    }

    @GetMapping("/doctors/recommend")
    @Operation(summary = "???????????-???? + ?????? + ?? + ????")
    public Result<Object> recommend(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false, defaultValue = "") String symptoms) {
        return Result.ok(service.recommend(userId, symptoms));
    }

    @PostMapping("/doctors/recommend-and-book")
    @Operation(summary = "??????????????????????????")
    public Result<Object> recommendAndBook(
            @AuthenticationPrincipal String userId,
            @RequestBody TriageBookRequest req) {
        if (userId == null) throw BizException.unauthorized("Please login first");
        User u = userRepository.findById(userId)
                .orElseThrow(() -> BizException.notFound("User not found"));
        if (u.getName() == null || u.getName().isBlank()) {
            throw BizException.badRequest("Please verify your real name before booking");
        }
        AppointmentDtos.AppointmentDto booked = service.bookFromTriage(
                userId, u.getName(), req == null ? "" : req.getSymptoms());
        if (booked == null) {
            return Result.fail(404, "?????????????????");
        }
        return Result.ok(booked);
    }

    @Data
    public static class TriageBookRequest {
        private String symptoms;
    }

    // ---------- my appointments ----------

    @PostMapping("/appointments")
    @Operation(summary = "Create appointment")
    public Result<Object> book(@AuthenticationPrincipal String userId, @RequestBody AppointmentDtos.BookRequest req) {
        if (userId == null) throw BizException.unauthorized("Please login first");
        req.setPatientId(userId);
        // Always derive patientName from the authenticated user; never trust client input.
        User u = userRepository.findById(userId)
                .orElseThrow(() -> BizException.notFound("User not found"));
        if (u.getName() == null || u.getName().isBlank()) {
            throw BizException.badRequest("Please verify your real name before booking");
        }
        req.setPatientName(u.getName());
        return Result.ok(service.book(req));
    }

    @GetMapping("/appointments/mine")
    @Operation(summary = "My appointments")
    public Result<Object> myAppointments(@AuthenticationPrincipal String userId) {
        return Result.ok(service.myAppointments(userId));
    }

    @PostMapping("/appointments/{id}/cancel")
    @Operation(summary = "Cancel appointment")
    public Result<Void> cancel(@AuthenticationPrincipal String userId, @PathVariable String id) {
        service.cancel(id, userId);
        return Result.ok(null);
    }

    @PostMapping("/appointments/{id}/pay")
    @Operation(summary = "Pay (dev: just set status to payed)")
    public Result<Void> pay(@AuthenticationPrincipal String userId, @PathVariable String id) {
        service.pay(id, userId);
        return Result.ok(null);
    }

    @PostMapping("/appointments/{id}/no-show")
    @Operation(summary = "Mark as no-show")
    public Result<Void> noShow(@AuthenticationPrincipal String userId, @PathVariable String id) {
        service.noShow(id, userId);
        return Result.ok(null);
    }

    @PostMapping("/appointments/{id}/reminder")
    @Operation(summary = "Toggle reminder")
    public Result<Void> setReminder(
            @AuthenticationPrincipal String userId,
            @PathVariable String id,
            @RequestParam boolean on) {
        service.setReminder(id, userId, on);
        return Result.ok(null);
    }

    @GetMapping("/appointments/no-show-count")
    @Operation(summary = "My no-show count")
    public Result<Integer> noShowCount(@AuthenticationPrincipal String userId) {
        return Result.ok(service.noShowCount(userId));
    }
}