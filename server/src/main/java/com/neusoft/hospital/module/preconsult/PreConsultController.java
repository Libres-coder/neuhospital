package com.neusoft.hospital.module.preconsult;

import com.neusoft.hospital.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/preconsult")
@RequiredArgsConstructor
@Tag(name = "PreConsult", description = "AI 预问诊 / 分诊")
public class PreConsultController {

    private final TriageService triageService;

    @PostMapping("/triage")
    @Operation(summary = "分诊：症状列表 → 推荐科室 + 可能疾病")
    public Result<TriageResponse> triage(@RequestBody TriageRequest req) {
        List<String> symptoms = req.getSymptoms() == null ? List.of() : req.getSymptoms();
        return Result.ok(new TriageResponse(
                triageService.possibleDiseases(symptoms),
                triageService.recommend(symptoms)));
    }

    @Data
    public static class TriageRequest {
        private List<String> symptoms;
    }

    @Data
    @lombok.AllArgsConstructor
    public static class TriageResponse {
        private List<Map<String, Object>> possibleDiseases;
        private List<Map<String, Object>> recommendedDepartments;
    }
}