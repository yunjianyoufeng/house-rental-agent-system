package com.rental.service;

import com.rental.entity.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class TokenService {

    private static final String TOKEN_PREFIX = "login:token:";
    private static final Duration TOKEN_EXPIRE = Duration.ofDays(1);
    public static final Duration ABSOLUTE_EXPIRE = Duration.ofDays(7);
    private static final DefaultRedisScript<Long> REFRESH = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('EXPIRE', KEYS[1], ARGV[2])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final Clock clock;

    @Autowired
    public TokenService(StringRedisTemplate stringRedisTemplate) {
        this(stringRedisTemplate, Clock.systemUTC());
    }

    TokenService(StringRedisTemplate stringRedisTemplate, Clock clock) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.clock = clock;
    }

    public String createToken(SysUser user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String redisKey = TOKEN_PREFIX + token;

        // 记录不可续期的签发时间；角色和禁用状态仍以数据库为准。
        stringRedisTemplate.opsForValue().set(redisKey, user.getId() + ":" + clock.instant().getEpochSecond(), TOKEN_EXPIRE);

        return token;
    }

    public Long getUserIdByToken(String token) {
        if (!validToken(token)) {
            return null;
        }

        String redisKey = TOKEN_PREFIX + token;
        String session = stringRedisTemplate.opsForValue().get(redisKey);
        if (remainingSeconds(session) <= 0) {
            return null;
        }
        return Long.valueOf(session.split(":")[0]);
    }

    public void deleteToken(String token) {
        if (!validToken(token)) {
            return;
        }

        stringRedisTemplate.delete(TOKEN_PREFIX + token);
    }

    public void refreshToken(String token) {
        if (!validToken(token)) {
            return;
        }

        String redisKey = TOKEN_PREFIX + token;
        String session = stringRedisTemplate.opsForValue().get(redisKey);
        long remaining = remainingSeconds(session);
        if (remaining > 0) {
            // 原子比较后续期，退出登录与续期并发时不会复活已删除的会话。
            stringRedisTemplate.execute(REFRESH, List.of(redisKey), session,
                    Long.toString(Math.min(remaining, TOKEN_EXPIRE.toSeconds())));
        }
    }

    private boolean validToken(String token) {
        return token != null && token.matches("[a-f0-9]{32}");
    }

    private long remainingSeconds(String session) {
        if (session == null || !session.matches("[1-9][0-9]*:[0-9]+")) {
            return 0; // 旧版没有签发时间的会话必须重新登录。
        }
        try {
            String[] parts = session.split(":");
            Long.parseLong(parts[0]);
            long age = clock.instant().getEpochSecond() - Long.parseLong(parts[1]);
            return age < 0 ? 0 : Math.max(0, ABSOLUTE_EXPIRE.toSeconds() - age);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
