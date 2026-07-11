package com.neusoft.hospital.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Server-side bookkeeping for refresh tokens.
 *
 *   - On refresh rotation we record the previous refresh's jti as "rotated" → single-use token.
 *   - On /api/auth/logout we record the current refresh jti as "revoked".
 *   - On read, we reject any jti that has been rotated or revoked.
 *
 * Caffeine entries expire at the refresh-token TTL so the map self-cleans.
 */
@Component
public class RefreshTokenStore {

    enum Status { ACTIVE, ROTATED, REVOKED }

    private final Cache<String, Status> jtiStatus;

    public RefreshTokenStore(
            @Value("${app.jwt.refresh-ttl-seconds:604800}") long refreshTtlSeconds,
            @Value("${app.ratelimit.max-keys:50000}") long maxKeys
    ) {
        this.jtiStatus = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(refreshTtlSeconds))
                .maximumSize(maxKeys)
                .build();
    }

    public void markActive(String jti) { jtiStatus.put(jti, Status.ACTIVE); }
    public void markRotated(String jti) { jtiStatus.put(jti, Status.ROTATED); }
    public void markRevoked(String jti) { jtiStatus.put(jti, Status.REVOKED); }

    public Status get(String jti) { return jtiStatus.getIfPresent(jti); }

    public boolean isSingleUse(String jti) {
        Status s = jtiStatus.getIfPresent(jti);
        return s == Status.ROTATED || s == Status.REVOKED;
    }
}
