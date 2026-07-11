package com.neusoft.hospital.module.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.neusoft.hospital.common.BizException;
import com.neusoft.hospital.common.IdCardValidator;
import com.neusoft.hospital.common.RateLimiter;
import com.neusoft.hospital.config.JwtUtil;
import com.neusoft.hospital.config.RefreshTokenStore;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenStore refreshStore;
    private final RateLimiter rateLimiter;

    @Value("${app.sms.fixed-code}")
    private String fixedCode;

    @Value("${app.sms.ttl-seconds}")
    private int smsTtlSeconds;

    @Value("${app.ratelimit.sms.per-phone-max:5}")
    private int smsPerPhoneMax;

    @Value("${app.ratelimit.sms.per-phone-window-seconds:60}")
    private int smsPerPhoneWindow;

    @Value("${app.ratelimit.sms.per-ip-max:20}")
    private int smsPerIpMax;

    @Value("${app.ratelimit.sms.per-ip-window-seconds:60}")
    private int smsPerIpWindow;

    // phone -> code (dev only)
    private final Cache<String, String> smsCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(10_000)
            .build();

    public AuthDtos.SmsResponse sendSms(String phone) {
        validatePhone(phone);

        // Two-dimensional rate limit: per-phone (real-user safety) and per-IP (anti-script).
        // Either trip → reject; both must pass to issue.
        if (!rateLimiter.tryAcquire("sms:phone:" + phone, smsPerPhoneMax, smsPerPhoneWindow)) {
            throw BizException.tooManyRequests("该手机号请求过于频繁，请稍后再试");
        }
        String ip = clientIp();
        if (ip != null &&
                !rateLimiter.tryAcquire("sms:ip:" + ip, smsPerIpMax, smsPerIpWindow)) {
            throw BizException.tooManyRequests("请求来源过于频繁，请稍后再试");
        }

        smsCache.put(phone, fixedCode);
        return new AuthDtos.SmsResponse(UUID.randomUUID().toString(), smsTtlSeconds);
    }

    private static String clientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest req = attrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                int comma = xff.indexOf(',');
                return (comma > 0 ? xff.substring(0, comma) : xff).trim();
            }
            return req.getRemoteAddr();
        } catch (Exception ignored) {
            return null;
        }
    }

    @Transactional
    public AuthDtos.LoginResponse login(String phone, String code) {
        validatePhone(phone);
        // Login is rate-limited separately from SMS so that credential stuffing can't bypass
        // the SMS budget and brute-force its way past the dev "123456" code.
        if (!rateLimiter.tryAcquire("login:phone:" + phone,
                Math.max(5, smsPerPhoneMax), smsPerPhoneWindow)) {
            throw BizException.tooManyRequests("登录尝试过于频繁，请稍后再试");
        }
        String expected = smsCache.getIfPresent(phone);
        if (expected == null || !expected.equals(code)) {
            throw new BizException(4001, "验证码错误或已过期");
        }
        smsCache.invalidate(phone);

        User user = userRepository.findByPhone(phone).orElseGet(() -> {
            String uid = "u_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            String displayName = "用户_" + phone.substring(phone.length() - 4);
            return userRepository.save(new User(uid, phone, displayName));
        });

        return buildLoginResponse(user);
    }

    private AuthDtos.LoginResponse buildLoginResponse(User user) {
        String access = jwtUtil.issueAccess(user.getId(), user.getPhone());
        JwtUtil.Issued refresh = jwtUtil.issueRefreshWithJti(user.getId());
        // Remember this refresh jti as currently-active so re-use is detectable.
        refreshStore.markActive(refresh.jti);
        return new AuthDtos.LoginResponse(
                access, refresh.token, user.getId(), user.getPhone(), user.getName(),
                user.isVerified(), user.isEhsBound(),
                jwtUtil.getAccessTtlSeconds(), jwtUtil.getRefreshTtlSeconds());
    }

    public AuthDtos.MeResponse me(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BizException.notFound("用户不存在"));
        return new AuthDtos.MeResponse(
                user.getId(), user.getPhone(), user.getName(),
                user.isVerified(), user.isEhsBound(), user.isMiBound());
    }

    @Transactional
    public AuthDtos.VerifyIdCardResponse verifyIdCard(String userId, String name, String idCard) {
        if (name == null || name.isBlank()) {
            throw BizException.badRequest("姓名不能为空");
        }
        if (idCard == null) {
            throw BizException.badRequest("身份证号不能为空");
        }
        String trimmed = idCard.trim().toUpperCase();
        if (!IdCardValidator.isValid(trimmed)) {
            // Don't expose internal validator reasons — just say "格式不正确".
            throw BizException.badRequest("身份证号格式不正确");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BizException.notFound("用户不存在"));
        user.setName(name);
        user.setVerified(true);
        user.setUpdateTime(System.currentTimeMillis());
        userRepository.save(user);
        return new AuthDtos.VerifyIdCardResponse(true);
    }

    @Transactional
    public AuthDtos.BindEhsResponse bindEhs(String userId, String cardNo) {
        if (cardNo == null || cardNo.isBlank()) {
            throw BizException.badRequest("电子健康卡号不能为空");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BizException.notFound("用户不存在"));
        user.setEhsCardNo(cardNo);
        user.setEhsBound(true);
        user.setUpdateTime(System.currentTimeMillis());
        userRepository.save(user);
        return new AuthDtos.BindEhsResponse(cardNo);
    }

    /**
     * Bind the user's national medical-insurance e-voucher. The number
     * format accepted here is the 20-digit 国家医保编码 or the user's
     * own ID-card number (the 国家医保 app accepts both).
     */
    @Transactional
    public AuthDtos.BindMiResponse bindMi(String userId, String medicalInsuranceNo) {
        if (medicalInsuranceNo == null) {
            throw BizException.badRequest("医保凭证号不能为空");
        }
        String mi = medicalInsuranceNo.trim();
        if (!(mi.matches("\\d{20}") || mi.matches("\\d{17}[\\dXx]"))) {
            throw BizException.badRequest("医保凭证号格式不正确（20 位国家医保编码或身份证号）");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BizException.notFound("用户不存在"));
        long now = System.currentTimeMillis();
        user.setMedicalInsuranceNo(mi);
        user.setMiBound(true);
        user.setMiBoundTime(now);
        user.setUpdateTime(now);
        userRepository.save(user);
        return new AuthDtos.BindMiResponse(mi, now);
    }

    public void logout(String userId, String refreshToken) {
        // Mark the current refresh jti revoked. Subsequent reuse will fail.
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                Claims c = jwtUtil.parse(refreshToken);
                Object jti = c.getId();
                if (jti != null) refreshStore.markRevoked(jti.toString());
            } catch (Exception ignored) {
                // expired / malformed refresh at logout time is fine — caller already leaving.
            }
        }
    }

    /**
     * Mint a new (access, refresh) pair from a valid refresh token. The old refresh is
     * marked ROTATED so a re-use attempt is observable.
     */
    public AuthDtos.LoginResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw BizException.badRequest("refresh token 不能为空");
        }
        Claims c;
        try {
            c = jwtUtil.parse(refreshToken);
        } catch (Exception e) {
            throw BizException.unauthorized("refresh token 无效或已过期");
        }
        if (!JwtUtil.TYPE_REFRESH.equals(c.get("type"))) {
            throw BizException.unauthorized("refresh token 无效或已过期");
        }
        String userId = c.getSubject();
        if (userId != null &&
                !rateLimiter.tryAcquire("refresh:user:" + userId, 60, 60)) {
            throw BizException.tooManyRequests("刷新过于频繁，请稍后再试");
        }
        String jti = c.getId();
        if (jti != null && refreshStore.isSingleUse(jti)) {
            // Re-use after rotation/revoke = theft; tear down this user's other refreshes
            // could be done here for full paranoia, but at minimum we reject this one.
            throw BizException.unauthorized("refresh token 已失效，请重新登录");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BizException.notFound("用户不存在"));
        if (jti != null) refreshStore.markRotated(jti);
        return buildLoginResponse(user);
    }

    private void validatePhone(String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BizException(4002, "手机号格式错误");
        }
    }
}