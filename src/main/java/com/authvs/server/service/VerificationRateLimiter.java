package com.authvs.server.service;

import com.authvs.server.util.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class VerificationRateLimiter {

    private final StringRedisTemplate redisTemplate;

    public String checkAndRecord(String channel, String target) {
        if (channel == null || target == null) {
            return "参数错误";
        }
        String c = channel.toUpperCase();
        String minuteKey = RedisKeys.mfaLimitMinute(c, target);
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(minuteKey, "1", Duration.ofMinutes(1));
        if (Boolean.FALSE.equals(ok)) {
            return "频率过高，请稍后再试";
        }
        String hourKey = RedisKeys.mfaLimitHour(c, target);
        Long cnt = redisTemplate.opsForValue().increment(hourKey);
        if (cnt != null && cnt == 1) {
            redisTemplate.expire(hourKey, Duration.ofHours(1));
        }
        if (cnt != null && cnt > 10) {
            redisTemplate.delete(minuteKey);
            return "一小时最多发送10条";
        }
        return null;
    }
}
