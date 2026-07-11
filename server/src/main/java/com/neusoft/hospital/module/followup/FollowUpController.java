package com.neusoft.hospital.module.followup;

import com.neusoft.hospital.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "FollowUp", description = "术后随访 + 慢病 + 康复")
public class FollowUpController {

    private final FollowUpService service;

    // ---------- plans ----------
    @PostMapping("/followup/plans")
    @Operation(summary = "创建术后随访计划（自动生成 7/14/30/60/90 天的任务）")
    public Result<FollowUpDtos.PlanDto> createPlan(@AuthenticationPrincipal String userId, @RequestBody FollowUpDtos.CreatePlanRequest req) {
        return Result.ok(service.createPlan(userId, req));
    }

    @GetMapping("/followup/plans")
    @Operation(summary = "我的随访计划列表")
    public Result<List<FollowUpDtos.PlanDto>> listPlans(@AuthenticationPrincipal String userId) {
        return Result.ok(service.listPlans(userId));
    }

    @GetMapping("/followup/plans/{id}/tasks")
    @Operation(summary = "指定计划的任务列表")
    public Result<List<FollowUpDtos.TaskDto>> listTasks(@AuthenticationPrincipal String userId, @PathVariable String id) {
        return Result.ok(service.listTasks(userId, id));
    }

    @GetMapping("/followup/tasks/pending")
    @Operation(summary = "我的待办任务")
    public Result<List<FollowUpDtos.TaskDto>> pending(@AuthenticationPrincipal String userId) {
        return Result.ok(service.pendingTasks(userId));
    }

    @PostMapping("/followup/tasks/complete")
    @Operation(summary = "完成任务并提交答案")
    public Result<Void> completeTask(@AuthenticationPrincipal String userId, @RequestBody FollowUpDtos.CompleteTaskRequest req) {
        service.completeTask(userId, req);
        return Result.ok(null);
    }

    // ---------- chronic ----------
    @GetMapping("/chronic/records")
    @Operation(summary = "我的慢病记录（按类型）")
    public Result<List<FollowUpDtos.ChronicRecordDto>> listChronic(@AuthenticationPrincipal String userId, @RequestParam String type) {
        return Result.ok(service.listRecords(userId, type));
    }

    @PostMapping("/chronic/records")
    @Operation(summary = "提交一条慢病记录（自动判断告警等级）")
    public Result<FollowUpDtos.ChronicRecordDto> submitChronic(@AuthenticationPrincipal String userId, @RequestBody FollowUpDtos.ChronicRecordRequest req) {
        return Result.ok(service.submitChronicRecord(userId, req));
    }

    @GetMapping("/chronic/alerts")
    @Operation(summary = "我的未确认告警")
    public Result<List<FollowUpDtos.ChronicAlertDto>> listAlerts(@AuthenticationPrincipal String userId) {
        return Result.ok(service.listAlerts(userId));
    }

    @PostMapping("/chronic/alerts/{id}/ack")
    @Operation(summary = "确认一条告警")
    public Result<Void> ackAlert(@AuthenticationPrincipal String userId, @PathVariable String id) {
        service.ackAlert(userId, id);
        return Result.ok(null);
    }

    // ---------- rehab ----------
    @GetMapping("/rehab/logs")
    @Operation(summary = "我的康复日志")
    public Result<List<FollowUpDtos.RehabLogDto>> listRehab(@AuthenticationPrincipal String userId) {
        return Result.ok(service.listRehab(userId));
    }

    @PostMapping("/rehab/logs")
    @Operation(summary = "创建康复日志")
    public Result<FollowUpDtos.RehabLogDto> createRehab(@AuthenticationPrincipal String userId, @RequestBody FollowUpDtos.CreateRehabRequest req) {
        return Result.ok(service.createRehab(userId, req));
    }

    @PostMapping("/rehab/logs/{id}/complete")
    @Operation(summary = "完成一条康复日志")
    public Result<Void> completeRehab(@AuthenticationPrincipal String userId, @PathVariable String id) {
        service.completeRehab(userId, id);
        return Result.ok(null);
    }
}