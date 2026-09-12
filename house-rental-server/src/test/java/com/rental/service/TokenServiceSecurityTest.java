package com.rental.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TokenServiceSecurityTest {
    private static final String TOKEN = "a".repeat(32);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final Instant now = Instant.parse("2026-09-12T00:00:00Z");
    private final TokenService service = new TokenService(redis, Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void legacyAndExpiredSessionsAreRejected() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenReturn("8", "8:" + (now.getEpochSecond() - 604800));
        assertNull(service.getUserIdByToken(TOKEN));
        assertNull(service.getUserIdByToken(TOKEN));
    }

    @Test
    void refreshNeverExceedsAbsoluteLifetime() {
        when(redis.opsForValue()).thenReturn(values);
        String session = "8:" + (now.getEpochSecond() - 604790);
        when(values.get(anyString())).thenReturn(session);
        assertEquals(8L, service.getUserIdByToken(TOKEN));
        service.refreshToken(TOKEN);
        verify(redis).execute(any(RedisScript.class), eq(List.of("login:token:" + TOKEN)), eq(session), eq("10"));
    }

    @Test
    void malformedTokensNeverReachRedis() {
        assertNull(service.getUserIdByToken("untrusted-key"));
        verifyNoInteractions(redis);
    }
}
