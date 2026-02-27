package com.example.lablink.domain.oauth.service;

import com.example.lablink.domain.oauth.enums.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OAuthStateStore {

    private static final String KEY_PREFIX = "oauth_state:";
    private static final long STATE_TTL_MINUTES = 5;

    private final RedisTemplate<String, Object> redisTemplate;

    public String generateState(OAuthProvider provider) {
        String state = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(KEY_PREFIX + state, provider.name(), STATE_TTL_MINUTES, TimeUnit.MINUTES);
        return state;
    }

    public boolean validateAndConsume(String state, OAuthProvider provider) {
        String key = KEY_PREFIX + state;
        Object stored = redisTemplate.opsForValue().getAndDelete(key);
        return provider.name().equals(stored);
    }
}
