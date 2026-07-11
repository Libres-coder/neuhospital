package com.neusoft.hospital.common;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Fixed-window rate limiter backed by Caffeine. Suitable for SMS / AI bot-spam defense on a
 * single-instance deployment. For multi-instance billing-grade limits, swap to a Redis Lua
 * script via the same interface.
 *
 * Design trade-off: we keep one (count, windowStartMs) pair per key. When the window expires
 * we lazy-reset on the next call. This is correct (no over-allowance beyond one window's
 * worth, no under-allowance) at the cost of edges where the user can spend `maxPerWindow`
 * tokens in 1ms at window rollover. For our use case (SMS 5/min, AI 30/min) that's fine.
 */
@Component
public class RateLimiter {

    public static final class Window {
        final AtomicReference<long[]> ref = new AtomicReference<>(new long[]{0L, 0L});

        boolean tryAcquire(int max, long windowMs, long now) {
            while (true) {
                long[] cur = ref.get();
                long start = cur[1];
                if (now - start >= windowMs) {
                    long[] fresh = {0L, now};
                    if (!ref.compareAndSet(cur, fresh)) continue;
                    cur = fresh;
                }
                if (cur[0] >= max) return false;
                long[] next = {cur[0] + 1, cur[1]};
                if (ref.compareAndSet(cur, next)) return true;
                // CAS lost — another thread raced us, retry
            }
        }
    }

    private final Cache<String, Window> windows;

    public RateLimiter(
            @Value("${app.ratelimit.max-keys:50000}") long maxKeys,
            @Value("${app.ratelimit.idle-minutes:30}") long idleMinutes
    ) {
        this.windows = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(idleMinutes))
                .maximumSize(maxKeys)
                .build();
    }

    /**
     * @return true if the call is allowed (and a token is consumed), false if the limit is hit.
     */
    public boolean tryAcquire(String key, int maxPerWindow, int windowSeconds) {
        Window w = windows.get(key, k -> new Window());
        return w.tryAcquire(maxPerWindow, windowSeconds * 1000L, System.currentTimeMillis());
    }
}
