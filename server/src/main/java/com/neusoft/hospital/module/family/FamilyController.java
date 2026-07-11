package com.neusoft.hospital.module.family;

import com.neusoft.hospital.common.BizException;
import com.neusoft.hospital.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/family")
@RequiredArgsConstructor
@Tag(name = "Family", description = "亲情账户 — 老人/儿童代挂号")
public class FamilyController {

    private final FamilyService service;

    @GetMapping
    @Operation(summary = "我的亲情账户列表（首次自动建本人）")
    public Result<FamilyDtos.ListResponse> list(@AuthenticationPrincipal String userId) {
        if (userId == null) throw BizException.unauthorized("请先登录");
        return Result.ok(service.listAndAutoSeed(userId));
    }

    @PostMapping
    @Operation(summary = "新增亲情账户")
    public Result<FamilyDtos.FamilyMemberDto> add(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody FamilyDtos.AddRequest req) {
        return Result.ok(service.add(userId, req));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "更新亲情账户")
    public Result<FamilyDtos.FamilyMemberDto> update(
            @AuthenticationPrincipal String userId,
            @PathVariable String id,
            @Valid @RequestBody FamilyDtos.UpdateRequest req) {
        return Result.ok(service.update(userId, id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除亲情账户")
    public Result<Void> delete(@AuthenticationPrincipal String userId, @PathVariable String id) {
        service.delete(userId, id);
        return Result.ok(null);
    }

    @PostMapping("/{id}/default")
    @Operation(summary = "设为当前挂号人（切到该亲情账户代挂号）")
    public Result<FamilyDtos.FamilyMemberDto> setDefault(
            @AuthenticationPrincipal String userId,
            @PathVariable String id) {
        return Result.ok(service.setDefault(userId, id));
    }
}
