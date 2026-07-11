package com.neusoft.hospital.module.followup;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.hospital.module.aichat.BailianClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI helpers for the follow-up / chronic-disease module. Each method has a
 * local deterministic fallback so demo mode works without an API key.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowUpAiService {

    private final BailianClient bailian;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Build a per-disease question list using the vision/chat model.
     * Falls back to a deterministic template (keyed on {@code disease}) if
     * Bailian is unavailable.
     */
    public List<String> personalizedQuestions(String disease, int dayIndex) {
        String prompt = String.format(
                "你是临床随访助手。病人诊断为「%s」，今天是术后第%d天。" +
                        "请基于这个阶段最常见的康复关注点，输出 4-6 个随访问题，" +
                        "覆盖伤口/疼痛/用药/活动/心理/饮食。要求简洁具体，每个一行，只输出问题，不要编号不要其它说明。",
                disease, dayIndex);
        try {
            String out = bailian.chat("你是临床随访助手。回答简短、结构化、不要重复寒暄。", prompt);
            List<String> parsed = parseLines(out);
            if (parsed.size() >= 3) return parsed.subList(0, Math.min(6, parsed.size()));
            return fallbackQuestions(disease, dayIndex);
        } catch (Exception e) {
            return fallbackQuestions(disease, dayIndex);
        }
    }

    /**
     * Generate a doctor's reply based on the answers the patient submitted.
     * Falls back to a deterministic sentence so the UI still gets something
     * readable when Bailian is unavailable.
     */
    public String doctorReply(String disease, List<String> questions, Map<String, String> answers) {
        StringBuilder sb = new StringBuilder();
        sb.append("疾病:").append(disease).append('\n');
        if (questions != null) {
            for (String q : questions) {
                sb.append("Q:").append(q).append('\n');
                sb.append("A:").append(answers == null ? "—" : answers.getOrDefault(q, "—")).append('\n');
            }
        }
        try {
            String out = bailian.chat(
                    "你是一位经验丰富的康复科医生。回复病人要温和、具体、可执行；不超过120字；如出现高风险信号请立即建议就医。",
                    sb.toString());
            return out == null || out.isBlank()
                    ? "已收到您的反馈。请继续按医嘱康复，注意休息并按时复查。如出现异常请立即就医。"
                    : out.trim();
        } catch (Exception e) {
            return "已收到您的反馈。请继续按医嘱康复，注意休息并按时复查。如出现异常请立即就医。";
        }
    }

    /**
     * AI-assisted chronic-disease classification. Returns alert level 0..3,
     * and when level &gt;= 2, a Chinese-language message for the patient.
     * Caller should pass the latest record AND a few prior records (newest
     * first) so the model can spot trends, not just absolutes.
     */
    public AiAlertResult classifyChronic(String type, Map<String, Object> latest, List<Map<String, Object>> recent) {
        String prompt = buildChronicPrompt(type, latest, recent);
        try {
            String out = bailian.chat(
                    "你是慢病管理医生。给定一组最近的测量记录，请判断患者状态是否正常。" +
                            "回复必须是纯 JSON，格式 {\"level\":0|1|2|3,\"message\":\"...\"}，" +
                            "level 0 正常、1 注意、2 警告、3 危险。不要任何额外文字。",
                    prompt);
            AiAlertResult parsed = parseAlertJson(out);
            return parsed != null ? parsed : thresholdFallback(type, latest);
        } catch (Exception e) {
            return thresholdFallback(type, latest);
        }
    }

    // ---------- helpers ----------

    private List<String> fallbackQuestions(String disease, int dayIndex) {
        // Stage-based templates keyed on a simple keyword match.
        Map<String, List<String>> template = diseaseTemplate(disease);
        if (dayIndex <= 7) return template.get("week1");
        if (dayIndex <= 30) return template.get("month1");
        return template.get("after");
    }

    private Map<String, List<String>> diseaseTemplate(String disease) {
        String d = disease == null ? "" : disease;
        if (d.contains("骨") || d.contains("关节") || d.contains("骨折")) {
            return Map.of(
                    "week1", List.of("伤口是否有红肿、渗液？", "疼痛评分（0-10）", "是否按时服用抗生素/止痛药？", "能否在协助下扶拐行走？"),
                    "month1", List.of("行走距离是否有改善？", "是否出现关节僵硬？", "康复锻炼完成情况", "睡眠与情绪"),
                    "after", List.of("日常活动是否完全恢复？", "是否仍有疼痛？", "是否有计划复查X线？"));
        }
        if (d.contains("心脏") || d.contains("心") || d.contains("搭桥") || d.contains("支架")) {
            return Map.of(
                    "week1", List.of("胸痛/胸闷是否出现？", "是否规律服用抗凝药？", "伤口有无异常？", "静息心率"),
                    "month1", List.of("活动耐量（步行多少米不喘）", "是否规律监测血压心率", "低盐低脂饮食执行情况", "睡眠与情绪"),
                    "after", List.of("是否完成心脏康复训练？", "近期是否有心慌胸闷？", "下次门诊复查时间？"));
        }
        if (d.contains("胃") || d.contains("消化") || d.contains("肠")) {
            return Map.of(
                    "week1", List.of("腹部疼痛评分", "是否出现黑便/呕血？", "食欲与进食量", "是否按时服药"),
                    "month1", List.of("大便习惯", "是否仍有腹痛/反酸", "饮食结构", "体重变化"),
                    "after", List.of("是否复查胃镜/肠镜？", "饮食习惯是否已调整", "是否有贫血症状"));
        }
        // default template (same as the original 5-item list, kept for compatibility)
        return Map.of(
                "week1", List.of("伤口愈合情况", "疼痛评分（0-10）", "用药情况", "精神状态", "饮食情况"),
                "month1", List.of("症状变化", "是否按时服药", "活动恢复情况", "精神状态", "饮食与睡眠"),
                "after", List.of("是否完全康复", "是否计划复查", "长期用药依从性", "生活方式调整"));
    }

    private AiAlertResult thresholdFallback(String type, Map<String, Object> latest) {
        int level = 0;
        StringBuilder msg = new StringBuilder();
        if ("HYPERTENSION".equalsIgnoreCase(type)) {
            Object sObj = latest.get("systolic");
            Object dObj = latest.get("diastolic");
            int s = sObj instanceof Number ? ((Number) sObj).intValue() : 0;
            int d = dObj instanceof Number ? ((Number) dObj).intValue() : 0;
            if (s >= 180 || d >= 110) level = 3;
            else if (s >= 160 || d >= 100) level = 2;
            else if (s >= 140 || d >= 90) level = 1;
            if (level > 0) msg.append("血压 ").append(s).append('/').append(d);
        } else if ("DIABETES".equalsIgnoreCase(type)) {
            Object fObj = latest.get("fastingGlucose");
            Object pObj = latest.get("postprandialGlucose");
            Object aObj = latest.get("hba1c");
            double f = fObj instanceof Number ? ((Number) fObj).doubleValue() : 0;
            double p = pObj instanceof Number ? ((Number) pObj).doubleValue() : 0;
            double a = aObj instanceof Number ? ((Number) aObj).doubleValue() : 0;
            if (f >= 13 || p >= 20 || a >= 10) level = 3;
            else if (f >= 10 || p >= 16 || a >= 8.5) level = 2;
            else if (f >= 7 || p >= 11.1 || a >= 7) level = 1;
            if (level > 0) msg.append("血糖异常（空腹").append(f).append("，餐后").append(p).append('）');
        }
        if (level == 0) return new AiAlertResult(0, "");
        String prefix = level == 3 ? "[危险]" : level == 2 ? "[警告]" : "[注意]";
        msg.insert(0, prefix);
        msg.append("，建议尽快就医或联系医生。");
        return new AiAlertResult(level, msg.toString());
    }

    private String buildChronicPrompt(String type, Map<String, Object> latest, List<Map<String, Object>> recent) {
        StringBuilder sb = new StringBuilder();
        sb.append("类型:").append(type).append('\n');
        sb.append("最近测量:").append(mapper.valueToTree(latest).toString()).append('\n');
        sb.append("近期历史(最多5条,新→旧):").append(mapper.valueToTree(recent).toString()).append('\n');
        sb.append("请基于以上数据判断患者当前状态是否异常。");
        return sb.toString();
    }

    private AiAlertResult parseAlertJson(String text) {
        if (text == null) return null;
        // The model sometimes wraps JSON in ```json ... ``` blocks; strip.
        String s = text.trim();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline > 0) s = s.substring(firstNewline + 1);
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
            s = s.trim();
        }
        try {
            Map<String, Object> m = mapper.readValue(s, new TypeReference<Map<String, Object>>() {});
            int level = ((Number) m.getOrDefault("level", 0)).intValue();
            String msg = String.valueOf(m.getOrDefault("message", ""));
            return new AiAlertResult(Math.max(0, Math.min(3, level)), msg);
        } catch (Exception e) {
            log.warn("parseAlertJson failed: {}", e.toString());
            return null;
        }
    }

    private List<String> parseLines(String text) {
        if (text == null || text.isBlank()) return List.of();
        String[] lines = text.split("\\r?\\n");
        List<String> out = new ArrayList<>();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            // strip leading bullet markers 1. / - / • / 1) / 【】
            line = line.replaceAll("^[\\-\\•\\*\\d\\.\\)\\:【】\\[\\]\\s]+", "");
            if (!line.isEmpty()) out.add(line);
        }
        return out;
    }

    public static class AiAlertResult {
        public final int level;
        public final String message;
        public AiAlertResult(int level, String message) {
            this.level = level;
            this.message = message;
        }
    }
}
