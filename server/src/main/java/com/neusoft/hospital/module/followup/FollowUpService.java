package com.neusoft.hospital.module.followup;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.hospital.common.BizException;
import com.neusoft.hospital.core.util.DateExt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowUpService {

    private final FollowUpPlanRepository planRepository;
    private final FollowUpTaskRepository taskRepository;
    private final ChronicRecordRepository chronicRecordRepository;
    private final ChronicAlertRepository chronicAlertRepository;
    private final RehabLogRepository rehabLogRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    // ---------- follow-up plans ----------

    @Transactional
    public FollowUpDtos.PlanDto createPlan(String userId, FollowUpDtos.CreatePlanRequest req) {
        long now = System.currentTimeMillis();
        FollowUpPlan plan = new FollowUpPlan();
        plan.setId("plan_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        plan.setPatientId(userId);
        plan.setDisease(req.getDisease());
        plan.setSurgeryDate(req.getSurgeryDate());
        plan.setTotalDays(req.getTotalDays());
        plan.setCreateTime(now);
        planRepository.save(plan);

        // Generate tasks at 7/14/30/60/90 day checkpoints
        List<Integer> checkpoints = Arrays.asList(7, 14, 30, 60, 90).stream()
                .filter(d -> d <= req.getTotalDays()).collect(Collectors.toList());
        List<String> baseQuestions = Arrays.asList("伤口愈合情况", "疼痛评分（0-10）", "用药情况", "精神状态", "饮食情况");
        String qjson = joinList(baseQuestions);

        List<FollowUpTask> tasks = new ArrayList<>();
        for (int day : checkpoints) {
            FollowUpTask t = new FollowUpTask();
            t.setId("task_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            t.setPlanId(plan.getId());
            t.setPatientId(userId);
            t.setDayIndex(day);
            t.setTargetDate(DateExt.addDays(plan.getSurgeryDate(), day));
            t.setQuestionsJson(qjson);
            t.setCompleted(false);
            tasks.add(t);
        }
        taskRepository.saveAll(tasks);

        return toPlanDto(plan, tasks);
    }

    public List<FollowUpDtos.PlanDto> listPlans(String userId) {
        return planRepository.findByPatientIdOrderByCreateTimeDesc(userId).stream()
                .map(p -> toPlanDto(p, taskRepository.findByPlanIdOrderByDayIndexAsc(p.getId())))
                .collect(Collectors.toList());
    }

    public List<FollowUpDtos.TaskDto> listTasks(String userId, String planId) {
        FollowUpPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> BizException.notFound("随访计划不存在"));
        if (!plan.getPatientId().equals(userId)) throw BizException.forbidden("无权访问该计划");
        return taskRepository.findByPlanIdOrderByDayIndexAsc(planId).stream()
                .map(this::toTaskDto).collect(Collectors.toList());
    }

    public List<FollowUpDtos.TaskDto> pendingTasks(String userId) {
        String today = DateExt.today();
        return taskRepository.findByPatientIdAndCompletedFalseAndTargetDateLessThanEqualOrderByTargetDateAsc(userId, today)
                .stream().map(this::toTaskDto).collect(Collectors.toList());
    }

    @Transactional
    public void completeTask(String userId, FollowUpDtos.CompleteTaskRequest req) {
        FollowUpTask task = taskRepository.findById(req.getTaskId())
                .orElseThrow(() -> BizException.notFound("任务不存在"));
        if (!task.getPatientId().equals(userId)) throw BizException.forbidden("无权操作该任务");
        try {
            task.setAnswersJson(mapper.writeValueAsString(req.getAnswers()));
        } catch (Exception e) {
            throw BizException.badRequest("answers format invalid");
        }
        task.setCompleted(true);
        task.setCompletedTime(System.currentTimeMillis());
        // canned doctor reply (mirror the previous client behaviour)
        task.setDoctorReply("已收到您的反馈。请继续按医嘱康复，注意休息并按时复查。如出现异常请立即就医。");
        taskRepository.save(task);
    }

    // ---------- chronic records ----------

    public List<FollowUpDtos.ChronicRecordDto> listRecords(String userId, String type) {
        return chronicRecordRepository.findByPatientIdAndTypeOrderByRecordDateDesc(userId, type).stream()
                .map(this::toChronicDto).collect(Collectors.toList());
    }

    @Transactional
    public FollowUpDtos.ChronicRecordDto submitChronicRecord(String userId, FollowUpDtos.ChronicRecordRequest req) {
        ChronicRecord r = new ChronicRecord();
        r.setId("cr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        r.setPatientId(userId);
        r.setType(req.getType());
        r.setRecordDate(req.getDate());
        r.setSystolic(req.getSystolic());
        r.setDiastolic(req.getDiastolic());
        r.setHeartRate(req.getHeartRate());
        r.setFastingGlucose(req.getFastingGlucose());
        r.setPostprandialGlucose(req.getPostprandialGlucose());
        r.setHba1c(req.getHba1c());
        r.setNote(req.getNote());

        int level = computeAlertLevel(req);
        r.setAlertLevel(level);
        r.setCreateTime(System.currentTimeMillis());
        chronicRecordRepository.save(r);

        if (level >= 2) {
            ChronicAlert a = new ChronicAlert();
            a.setId("alert_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            a.setPatientId(userId);
            a.setRecordId(r.getId());
            a.setType(req.getType());
            a.setLevel(level);
            a.setMessage(buildAlertMessage(req, level));
            a.setCreateTime(System.currentTimeMillis());
            a.setAcknowledged(false);
            chronicAlertRepository.save(a);
        }
        return toChronicDto(r);
    }

    public List<FollowUpDtos.ChronicAlertDto> listAlerts(String userId) {
        return chronicAlertRepository.findByPatientIdAndAcknowledgedFalseOrderByCreateTimeDesc(userId).stream()
                .map(this::toAlertDto).collect(Collectors.toList());
    }

    @Transactional
    public void ackAlert(String userId, String alertId) {
        ChronicAlert a = chronicAlertRepository.findById(alertId)
                .orElseThrow(() -> BizException.notFound("提醒不存在"));
        if (!a.getPatientId().equals(userId)) throw BizException.forbidden("无权操作该提醒");
        a.setAcknowledged(true);
        chronicAlertRepository.save(a);
    }

    // ---------- rehab logs ----------

    public List<FollowUpDtos.RehabLogDto> listRehab(String userId) {
        return rehabLogRepository.findByPatientIdOrderByLogDateDesc(userId).stream()
                .map(this::toRehabDto).collect(Collectors.toList());
    }

    @Transactional
    public FollowUpDtos.RehabLogDto createRehab(String userId, FollowUpDtos.CreateRehabRequest req) {
        RehabLog l = new RehabLog();
        l.setId("rehab_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        l.setPatientId(userId);
        l.setDisease(req.getDisease());
        l.setLogDate(DateExt.today());
        l.setItemsJson(joinList(req.getItems()));
        l.setCompleted(false);
        l.setCreateTime(System.currentTimeMillis());
        rehabLogRepository.save(l);
        return toRehabDto(l);
    }

    @Transactional
    public void completeRehab(String userId, String rehabId) {
        RehabLog l = rehabLogRepository.findById(rehabId)
                .orElseThrow(() -> BizException.notFound("康复记录不存在"));
        if (!l.getPatientId().equals(userId)) throw BizException.forbidden("无权操作该记录");
        l.setCompleted(true);
        rehabLogRepository.save(l);
    }

    // ---------- thresholds ----------

    private int computeAlertLevel(FollowUpDtos.ChronicRecordRequest r) {
        if ("HYPERTENSION".equalsIgnoreCase(r.getType())) {
            int s = r.getSystolic() == null ? 0 : r.getSystolic();
            int d = r.getDiastolic() == null ? 0 : r.getDiastolic();
            if (s >= 180 || d >= 110) return 3;
            if (s >= 160 || d >= 100) return 2;
            if (s >= 140 || d >= 90) return 1;
        } else if ("DIABETES".equalsIgnoreCase(r.getType())) {
            double f = r.getFastingGlucose() == null ? 0 : r.getFastingGlucose();
            double p = r.getPostprandialGlucose() == null ? 0 : r.getPostprandialGlucose();
            double a = r.getHba1c() == null ? 0 : r.getHba1c();
            if (f >= 13 || p >= 20 || a >= 10) return 3;
            if (f >= 10 || p >= 16 || a >= 8.5) return 2;
            if (f >= 7 || p >= 11.1 || a >= 7) return 1;
        }
        return 0;
    }

    private String buildAlertMessage(FollowUpDtos.ChronicRecordRequest r, int level) {
        String prefix = level == 3 ? "[危险]" : level == 2 ? "[警告]" : "[注意]";
        if ("HYPERTENSION".equalsIgnoreCase(r.getType())) {
            return prefix + "血压偏高（" + r.getSystolic() + "/" + r.getDiastolic() + "），建议尽快就医。";
        }
        if ("DIABETES".equalsIgnoreCase(r.getType())) {
            return prefix + "血糖异常（空腹" + r.getFastingGlucose() + "，餐后" + r.getPostprandialGlucose() + "），建议尽快就医。";
        }
        return prefix + "数据出现异常，请咨询医生。";
    }

    // ---------- helpers ----------

    private String joinList(List<String> items) {
        return String.join("|", items);
    }

    private FollowUpDtos.PlanDto toPlanDto(FollowUpPlan p, List<FollowUpTask> tasks) {
        return new FollowUpDtos.PlanDto(
                p.getId(), p.getPatientId(), p.getDisease(),
                p.getSurgeryDate(), p.getTotalDays(), p.getCreateTime(),
                tasks.stream().map(this::toTaskDto).collect(Collectors.toList()));
    }

    private FollowUpDtos.TaskDto toTaskDto(FollowUpTask t) {
        return new FollowUpDtos.TaskDto(
                t.getId(), t.getPlanId(), t.getDayIndex(), t.getTargetDate(),
                splitList(t.getQuestionsJson()),
                parseAnswers(t.getAnswersJson()),
                t.isCompleted(),
                t.getCompletedTime(),
                t.getDoctorReply());
    }

    private FollowUpDtos.ChronicRecordDto toChronicDto(ChronicRecord r) {
        return new FollowUpDtos.ChronicRecordDto(
                r.getId(), r.getType(), r.getRecordDate(),
                r.getSystolic(), r.getDiastolic(), r.getHeartRate(),
                r.getFastingGlucose(), r.getPostprandialGlucose(),
                r.getHba1c(), r.getNote(), r.getAlertLevel(), r.getCreateTime());
    }

    private FollowUpDtos.ChronicAlertDto toAlertDto(ChronicAlert a) {
        return new FollowUpDtos.ChronicAlertDto(
                a.getId(), a.getRecordId(), a.getType(),
                a.getLevel(), a.getMessage(), a.getCreateTime(), a.isAcknowledged());
    }

    private FollowUpDtos.RehabLogDto toRehabDto(RehabLog l) {
        return new FollowUpDtos.RehabLogDto(
                l.getId(), l.getDisease(), l.getLogDate(),
                splitList(l.getItemsJson()), l.isCompleted());
    }

    private List<String> splitList(String s) {
        if (s == null || s.isEmpty()) return List.of();
        return Arrays.asList(s.split("\\|"));
    }

    private Map<String, String> parseAnswers(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return mapper.readValue(s, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return null;
        }
    }
}