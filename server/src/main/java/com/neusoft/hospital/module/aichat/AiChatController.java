package com.neusoft.hospital.module.aichat;

import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.module.aichat.AiChatDtos.SessionSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aichat")
@RequiredArgsConstructor
@Tag(name = "AiChat", description = "AI 助手聊天")
public class AiChatController {

    private final AiChatService service;

    @PostMapping("/send")
    @Operation(summary = "发送消息；不带 sessionId 自动新建")
    public Result<AiChatDtos.SendResponse> send(@AuthenticationPrincipal String userId, @RequestBody AiChatDtos.SendRequest req) {
        return Result.ok(service.sendMessage(userId, req));
    }

    @GetMapping("/sessions")
    @Operation(summary = "我的会话列表")
    public Result<List<SessionSummary>> sessions(@AuthenticationPrincipal String userId) {
        return Result.ok(service.listSessions(userId));
    }

    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "删除会话")
    public Result<Void> deleteSession(@AuthenticationPrincipal String userId, @PathVariable String id) {
        service.deleteSession(userId, id);
        return Result.ok(null);
    }

    @GetMapping("/sessions/{id}/history")
    @Operation(summary = "会话历史")
    public Result<List<AiChatDtos.HistoryItem>> history(@AuthenticationPrincipal String userId, @PathVariable String id) {
        return Result.ok(service.getHistory(userId, id));
    }
}