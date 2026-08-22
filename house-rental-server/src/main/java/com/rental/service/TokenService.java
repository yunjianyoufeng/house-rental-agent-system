package com.rental.service;

import com.rental.entity.SysUser;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class TokenService {

    private static final String TOKEN_PREFIX = "login:token:";
    private static final Duration TOKEN_EXPIRE = Duration.ofDays(7);

    private final StringRedisTemplate stringRedisTemplate;

    public TokenService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String createToken(SysUser user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String redisKey = TOKEN_PREFIX + token;

        // 这里只先保存 userId，后面鉴权时再根据 userId 查数据库
        stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(user.getId()), TOKEN_EXPIRE);

        return token;
    }

    public Long getUserIdByToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String redisKey = TOKEN_PREFIX + token;
        String userId = stringRedisTemplate.opsForValue().get(redisKey);

        if (userId == null || userId.isBlank()) {
            return null;
        }

        return Long.valueOf(userId);
    }

    public void deleteToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        stringRedisTemplate.delete(TOKEN_PREFIX + token);
    }

    public void refreshToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        String redisKey = TOKEN_PREFIX + token;
        Boolean exists = stringRedisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(exists)) {
            stringRedisTemplate.expire(redisKey, TOKEN_EXPIRE);
        }
    }
}