package com.rental.service;

import com.rental.exception.RequestLimitException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

@Service
public class RequestLimitService {

    // 计数与过期时间在同一条脚本中设置，多实例部署下也不会产生永久限流键。
    private static final DefaultRedisScript<Long> COUNTER = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;

    public RequestLimitService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void check(String bucket, String identity, int limit, Duration window) {
        String key = "request-limit:" + bucket + ":" + digest(identity);
        Long count = redis.execute(COUNTER, List.of(key), Long.toString(window.toSeconds()));
        // Redis 故障交给全局异常处理，不能在配额服务异常时无限放行付费请求。
        if (count == null || count > limit) {
            throw new RequestLimitException(window.toSeconds());
        }
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
