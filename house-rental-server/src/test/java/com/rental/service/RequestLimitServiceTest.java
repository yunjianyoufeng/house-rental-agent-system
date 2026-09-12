package com.rental.service;

import com.rental.exception.RequestLimitException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RequestLimitServiceTest {
    @Test
    void blocksRequestAboveQuotaAndKeepsRetryInterval() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(11L);
        RequestLimitService service = new RequestLimitService(redis);
        RequestLimitException error = assertThrows(RequestLimitException.class,
                () -> service.check("login", "account", 10, Duration.ofMinutes(15)));
        assertEquals(900, error.getRetryAfterSeconds());
    }

    @Test
    void noResultFromRedisDoesNotAllowUnlimitedRequests() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        RequestLimitService service = new RequestLimitService(redis);
        assertThrows(RequestLimitException.class,
                () -> service.check("llm-user", "8", 20, Duration.ofHours(1)));
    }
}
