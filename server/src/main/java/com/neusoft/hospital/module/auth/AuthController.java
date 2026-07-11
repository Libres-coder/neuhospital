package com.neusoft.hospital.module.auth;

import com.neusoft.hospital.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "登录 / 验证码 / 用户信息")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sms")
    @Operation(summary = "发送短信验证码（开发模式固定 123456）")
    public Result<AuthDtos.SmsResponse> sendSms(@Valid @RequestBody AuthDtos.SmsRequest req) {
        return Result.ok(authService.sendSms(req.getPhone()));
    }

    @PostMapping("/login")
    @Operation(summary = "验证码登录（首次自动注册）")
    public Result<AuthDtos.LoginResponse> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        return Result.ok(authService.login(req.getPhone(), req.getCode()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "用 refresh token 换新的 (access, refresh)，原 refresh 标记为一次性")
    public Result<AuthDtos.LoginResponse> refresh(@RequestBody AuthDtos.RefreshRequest req) {
        return Result.ok(authService.refresh(req.getRefreshToken()));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前登录用户信息")
    public Result<AuthDtos.MeResponse> me(@AuthenticationPrincipal String userId) {
        return Result.ok(authService.me(userId));
    }

    @PostMapping("/verify-idcard")
    @Operation(summary = "实名认证（身份证 + 姓名）")
    public Result<AuthDtos.VerifyIdCardResponse> verifyIdCard(
            @AuthenticationPrincipal String userId,
            @RequestBody AuthDtos.IdCardRequest req) {
        return Result.ok(authService.verifyIdCard(userId, req.getName(), req.getIdCard()));
    }

    @PostMapping("/bind-ehs")
    @Operation(summary = "绑定电子健康卡")
    public Result<AuthDtos.BindEhsResponse> bindEhs(
            @AuthenticationPrincipal String userId,
            @RequestBody AuthDtos.BindEhsRequest req) {
        return Result.ok(authService.bindEhs(userId, req.getEhsCardNo()));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录（撤销 refresh token；客户端丢弃 access token）")
    public Result<Void> logout(
            @AuthenticationPrincipal String userId,
            @RequestBody(required = false) AuthDtos.RefreshRequest req) {
        String token = (req == null) ? null : req.getRefreshToken();
        authService.logout(userId, token);
        return Result.ok(null);
    }
}