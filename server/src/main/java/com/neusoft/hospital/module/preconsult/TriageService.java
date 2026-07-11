package com.neusoft.hospital.module.preconsult;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Local rules-based triage policy. Mirrors the previous Android-side [TriagePolicy]
 * so client behaviour is unchanged when the backend runs the same logic.
 */
@Service
public class TriageService {

    /** A map of symptom-key → list of {departmentName, score}. */
    private record DeptScore(String departmentName, double score) {}

    private static final Map<String, List<DeptScore>> SYMPTOM_MAP = new LinkedHashMap<>();
    static {
        put("胸痛",  List.of(new DeptScore("心血管内科", 0.9),  new DeptScore("呼吸内科", 0.5)));
        put("心悸",  List.of(new DeptScore("心血管内科", 0.95)));
        put("高血压", List.of(new DeptScore("心血管内科", 0.95)));
        put("气短",  List.of(new DeptScore("呼吸内科", 0.7),  new DeptScore("心血管内科", 0.6)));
        put("咳嗽",  List.of(new DeptScore("呼吸内科", 0.8)));
        put("哮喘",  List.of(new DeptScore("呼吸内科", 0.95)));
        put("胃痛",  List.of(new DeptScore("消化内科", 0.9)));
        put("腹泻",  List.of(new DeptScore("消化内科", 0.85)));
        put("骨折",  List.of(new DeptScore("骨科", 0.95)));
        put("关节",  List.of(new DeptScore("骨科", 0.85)));
        put("皮疹",  List.of(new DeptScore("皮肤科", 0.85)));
        put("过敏",  List.of(new DeptScore("皮肤科", 0.7),  new DeptScore("耳鼻喉科", 0.5)));
        put("视力",  List.of(new DeptScore("眼科", 0.9)));
        put("眼红",  List.of(new DeptScore("眼科", 0.85)));
        put("牙痛",  List.of(new DeptScore("口腔科", 0.9)));
        put("耳鸣",  List.of(new DeptScore("耳鼻喉科", 0.85)));
        put("失眠",  List.of(new DeptScore("中医科", 0.6),  new DeptScore("心血管内科", 0.4)));
        put("焦虑",  List.of(new DeptScore("中医科", 0.5)));
        put("儿童",  List.of(new DeptScore("儿科", 0.95)));
        put("发热",  List.of(new DeptScore("内科", 0.7),    new DeptScore("儿科", 0.85)));
        put("腹痛",  List.of(new DeptScore("消化内科", 0.8), new DeptScore("普通外科", 0.6)));
    }

    private static void put(String key, List<DeptScore> list) { SYMPTOM_MAP.put(key, list); }

    public List<Map<String, Object>> recommend(List<String> symptoms) {
        Map<String, Double> scores = new HashMap<>();
        for (String symptom : symptoms) {
            for (var e : SYMPTOM_MAP.entrySet()) {
                String key = e.getKey();
                boolean match = symptom.contains(key) || key.contains(symptom);
                if (!match) continue;
                for (DeptScore ds : e.getValue()) {
                    scores.merge(ds.departmentName(), ds.score(), Double::sum);
                }
            }
        }
        if (scores.isEmpty()) {
            scores.put("内科", 0.5);
        }
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .map(en -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("departmentName", en.getKey());
                    m.put("confidence", Math.min(1.0, en.getValue() / Math.max(1, symptoms.size())));
                    return m;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> possibleDiseases(List<String> symptoms) {
        List<String> matched = symptoms.stream()
                .map(s -> SYMPTOM_MAP.entrySet().stream()
                        .filter(e -> s.contains(e.getKey()) || e.getKey().contains(s))
                        .map(Map.Entry::getKey)
                        .findFirst().orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (matched.isEmpty()) {
            return List.of(Map.of(
                    "name", "暂无明确匹配",
                    "probability", 0.0,
                    "description", "建议描述更具体的症状或咨询医生"));
        }
        return matched.stream().map(name -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", name);
            m.put("probability", 0.6);
            m.put("description", "症状与\"" + name + "\"较为相关，建议尽快就医确诊。");
            return m;
        }).collect(Collectors.toList());
    }
}