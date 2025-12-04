package com.omnigaurd.solilos.service.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void cacheScanResult(Long scanId, Object result, long ttlMinutes) {
        try {
            String key = "scan:" + scanId;
            redisTemplate.opsForValue().set(key, result, ttlMinutes, TimeUnit.MINUTES);
            log.debug("Cached scan result for scanId: {}", scanId);
        } catch (Exception e) {
            log.error("Failed to cache scan result for scanId: {}", scanId, e);
        }
    }

    public Object getScanResult(Long scanId) {
        try {
            String key = "scan:" + scanId;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to get cached scan result for scanId: {}", scanId, e);
            return null;
        }
    }

    public void cacheFileContent(Long fileId, String content, long ttlMinutes) {
        try {
            String key = "file:" + fileId;
            redisTemplate.opsForValue().set(key, content, ttlMinutes, TimeUnit.MINUTES);
            log.debug("Cached file content for fileId: {}", fileId);
        } catch (Exception e) {
            log.error("Failed to cache file content for fileId: {}", fileId, e);
        }
    }

    public String getFileContent(Long fileId) {
        try {
            String key = "file:" + fileId;
            return (String) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to get cached file content for fileId: {}", fileId, e);
            return null;
        }
    }

    public void invalidateCache(String pattern) {
        try {
            redisTemplate.keys(pattern + "*").forEach(key -> redisTemplate.delete(key));
            log.debug("Invalidated cache for pattern: {}", pattern);
        } catch (Exception e) {
            log.error("Failed to invalidate cache for pattern: {}", pattern, e);
        }
    }
}
