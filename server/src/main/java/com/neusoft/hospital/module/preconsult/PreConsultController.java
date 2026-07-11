package com.neusoft.hospital.module.preconsult;

import com.neusoft.hospital.common.BizException;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.module.aichat.BailianClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/preconsult")
@RequiredArgsConstructor
@Tag(name = "PreConsult", description = "AI 预问诊 / 分诊")
public class PreConsultController {

    private final TriageService triageService;
    private final BailianClient bailian;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.public-base-url:http://localhost:8090}")
    private String publicBaseUrl;

    @Value("${app.upload.max-image-bytes:5242880}") // 5 MiB
    private long maxImageBytes;

    @PostMapping("/triage")
    @Operation(summary = "分诊：症状列表 → 推荐科室 + 可能疾病")
    public Result<TriageResponse> triage(@RequestBody TriageRequest req) {
        List<String> symptoms = req.getSymptoms() == null ? List.of() : req.getSymptoms();
        return Result.ok(new TriageResponse(
                triageService.possibleDiseases(symptoms),
                triageService.recommend(symptoms)));
    }

    /**
     * 图文分诊。Client POSTs a multipart/form-data with:
     *   - {@code file}: JPEG/PNG image
     *   - {@code symptoms}: optional text symptoms (multi-value or repeated)
     *
     * <p>Flow:
     * <ol>
     *   <li>save the upload under uploads/yyyy-MM-dd/uuid.{ext}</li>
     *   <li>hand the public URL to Bailian's vision model for description</li>
     *   <li>merge vision description with any user-supplied symptoms</li>
     *   <li>run the existing rules-based department recommender on the merged text</li>
     * </ol>
     *
     * <p>Falls back to the rules-only recommender if Bailian is unavailable.</p>
     */
    @PostMapping(value = "/triage/image", consumes = "multipart/form-data")
    @Operation(summary = "图文分诊：上传症状照片 + 可选文字症状")
    public Result<TriageResponse> triageImage(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "symptoms", required = false) List<String> symptoms
    ) throws IOException {
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("请上传症状照片");
        }
        if (file.getSize() > maxImageBytes) {
            throw BizException.badRequest("图片过大（最大 " + (maxImageBytes / 1024 / 1024) + " MB）");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!contentType.startsWith("image/")) {
            throw BizException.badRequest("仅支持图片文件");
        }
        String ext = contentType.endsWith("png") ? "png"
                : contentType.endsWith("webp") ? "webp"
                : contentType.endsWith("bmp") ? "bmp" : "jpg";

        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        Path dir = Paths.get(uploadDir, today).toAbsolutePath();
        Files.createDirectories(dir);
        String filename = UUID.randomUUID().toString().replace("-", "").substring(0, 16) + "." + ext;
        Path target = dir.resolve(filename);
        file.transferTo(target.toFile());

        String publicUrl = publicBaseUrl + "/uploads/" + today + "/" + filename;
        List<String> merged = new ArrayList<>();
        if (symptoms != null) merged.addAll(symptoms);
        try {
            String description = bailian.describeImage(publicUrl,
                    "请用中文描述这张图片里看到的医学相关症状（皮肤/口腔/舌苔/外伤等），不超过150字，结尾给出最可能的1-2个相关症状词。");
            merged.add(description);
        } catch (BailianClient.BailianUnavailableException | IllegalArgumentException e) {
            // proceed with whatever the user supplied
        }

        List<String> dedup = merged.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();

        List<Map<String, Object>> possible = triageService.possibleDiseases(dedup);
        List<Map<String, Object>> recs = triageService.recommend(dedup);
        return Result.ok(new TriageResponse(possible, recs));
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