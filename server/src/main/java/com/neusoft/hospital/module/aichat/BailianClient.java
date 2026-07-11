package com.neusoft.hospital.module.aichat;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Thin wrapper around the Alibaba Bailian (DashScope) Java SDK.
 *
 * <p>Why this exists: the previous {@link AiChatService#localReply} implementation
 * was a hand-written keyword matcher. The user's PRD explicitly requires
 * <em>阿里百炼</em>, not a placeholder. This client makes a real Qwen model call
 * via the official SDK and surfaces a clean {@link #chat(String, String)} API
 * for the chat service to consume.</p>
 *
 * <p>Fallback contract: callers should treat {@link BailianUnavailableException}
 * (or any other thrown runtime exception) as "model unreachable" and route to
 * the local fallback. We never want the chat endpoint to 500 because Bailian
 * had a hiccup.</p>
 */
@Slf4j
@Service
public class BailianClient {

    /** Maximum characters accepted in a single user message. Mirrors app.ai.max-content-length. */
    @Value("${app.ai.bailian.max-content-length:500}")
    private int maxContentLength;

    /** Bailian/DashScope API key. Set via env DASHSCOPE_API_KEY or BAILIAN_API_KEY. */
    @Value("${app.ai.bailian.api-key:}")
    private String apiKey;

    /** Model name. qwen-turbo is fast & cheap; switch to qwen-plus for higher quality. */
    @Value("${app.ai.bailian.model:qwen-turbo}")
    private String model;

    /** Generation timeout in milliseconds. */
    @Value("${app.ai.bailian.timeout-ms:15000}")
    private long timeoutMs;

    /** LLM temperature (0 = deterministic, 1 = creative). 0.4 keeps medical responses grounded. */
    @Value("${app.ai.bailian.temperature:0.4f}")
    private float temperature;

    /** Max output tokens. 800 ≈ ~600 Chinese chars, fits two-screen reply. */
    @Value("${app.ai.bailian.max-tokens:800}")
    private int maxTokens;

    /**
     * Whether the client is configured and ready to make calls.
     * Returns false if api-key is blank or model is blank.
     */
    public boolean isEnabled() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(model);
    }

    /**
     * Synchronous chat. Returns the assistant's reply text, or throws
     * {@link BailianUnavailableException} when the model call fails for any
     * reason the caller should treat as "service degraded".
     */
    public String chat(String systemPrompt, String userMessage) {
        if (!isEnabled()) {
            throw new BailianUnavailableException("Bailian client not configured (api-key missing)");
        }
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("userMessage is blank");
        }
        if (userMessage.length() > maxContentLength) {
            throw new IllegalArgumentException("userMessage exceeds " + maxContentLength + " chars");
        }

        String sys = StringUtils.hasText(systemPrompt) ? systemPrompt : "你是医疗助理，请简洁回答。";
        Message sysMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content(sys)
                .build();
        Message usrMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(userMessage)
                .build();
        List<Message> messages = Arrays.asList(sysMsg, usrMsg);

        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(model)
                .messages(messages)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();

        Generation gen = new Generation();
        try {
            // DashScope SDK doesn't expose a per-call timeout knob in 2.x — we wrap on our side
            // via a future if needed in the future. For now, rely on the SDK's default.
            long start = System.currentTimeMillis();
            GenerationResult result = gen.call(param);
            long cost = System.currentTimeMillis() - start;
            log.info("Bailian chat OK model={} costMs={}", model, cost);
            if (result == null || result.getOutput() == null
                    || result.getOutput().getChoices() == null
                    || result.getOutput().getChoices().isEmpty()) {
                throw new BailianUnavailableException("Bailian returned empty output");
            }
            Message assistant = result.getOutput().getChoices().get(0).getMessage();
            String content = assistant == null ? null : assistant.getContent();
            if (content == null || content.isBlank()) {
                throw new BailianUnavailableException("Bailian returned blank content");
            }
            return content.trim();
        } catch (NoApiKeyException e) {
            // missing/invalid api key — operator config issue
            log.error("Bailian NoApiKeyException: {}", e.getMessage());
            throw new BailianUnavailableException("Bailian API key invalid or missing", e);
        } catch (InputRequiredException e) {
            log.error("Bailian InputRequiredException: {}", e.getMessage());
            throw new BailianUnavailableException("Bailian rejected input", e);
        } catch (ApiException e) {
            // network / 4xx / 5xx from upstream
            String code = e.getStatus() == null ? "?" : e.getStatus().getCode();
            int httpCode = e.getStatus() == null ? -1 : e.getStatus().getStatusCode();
            log.warn("Bailian ApiException code={} httpStatus={} message={}", code, httpCode, e.getMessage());
            throw new BailianUnavailableException("Bailian API error (code=" + code + ", http=" + httpCode + ")", e);
        } catch (RuntimeException e) {
            // SDK throws RuntimeException for various transport failures
            log.warn("Bailian runtime failure: {}", e.toString());
            throw new BailianUnavailableException("Bailian transport failure", e);
        }
    }

    /**
     * Thrown when the Bailian model call cannot be completed. Callers should
     * fall back to local reply logic — do NOT surface this to end users as
     * a 5xx.
     */
    public static class BailianUnavailableException extends RuntimeException {
        public BailianUnavailableException(String message) { super(message); }
        public BailianUnavailableException(String message, Throwable cause) { super(message, cause); }
    }
}
