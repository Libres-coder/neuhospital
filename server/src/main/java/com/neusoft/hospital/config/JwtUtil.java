package com.neusoft.hospital.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Two-tier token issuer / parser.
 *
 *   access  — 15 min TTL, carries subject=userId, type="access". Used on every authenticated request.
 *   refresh — 7 day TTL, carries subject=userId, type="refresh", jti=<random>. Used only at
 *             POST /api/auth/refresh to issue a new access (and rotate the refresh).
 *
 * Both use the same HMAC key and the same secret; the secret must come from env in production.
 * If you ever need asymmetric (RS256 / Ed25519) keys for a public-key verifier, swap this
 * class without changing callers.
 */
@Component
public class JwtUtil {

    public static final String TYPE_ACCESS  = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final String issuer;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-ttl-seconds:900}") long accessTtlSeconds,
            @Value("${app.jwt.refresh-ttl-seconds:604800}") long refreshTtlSeconds,
            @Value("${app.jwt.issuer}") String issuer
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
        this.issuer = issuer;
    }

    public String issueAccess(String userId, String phone) {
        return buildToken(userId, phone, TYPE_ACCESS, accessTtlSeconds, null).token;
    }

    public String issueRefresh(String userId) {
        Issued t = buildToken(userId, null, TYPE_REFRESH, refreshTtlSeconds, UUID.randomUUID().toString());
        return t.token;
    }

    /** public so tests / refresh-store bookkeeping can see the jti we minted along the way. */
    public Issued issueRefreshWithJti(String userId) {
        return buildToken(userId, null, TYPE_REFRESH, refreshTtlSeconds, UUID.randomUUID().toString());
    }

    public static final class Issued {
        public final String token;
        public final String jti;
        public Issued(String token, String jti) { this.token = token; this.jti = jti; }
    }

    private Issued buildToken(String userId, String phone, String type,
                              long ttlSeconds, String jti) {
        long now = System.currentTimeMillis();
        var b = Jwts.builder()
                .issuer(issuer)
                .subject(userId)
                .claim("type", type)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlSeconds * 1000L));
        if (phone != null) b.claim("phone", phone);
        if (jti != null) b.id(jti);
        return new Issued(b.signWith(key).compact(), jti);
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long getAccessTtlSeconds()  { return accessTtlSeconds; }
    public long getRefreshTtlSeconds() { return refreshTtlSeconds; }
    public Instant nowPlusRefreshTtl() { return Instant.now().plusSeconds(refreshTtlSeconds); }
}
