package com.omnigaurd.solilos.service.scan;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScanQueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String SCAN_QUEUE = "scan_queue";
    private static final String SCAN_STATUS_PREFIX = "scan_status:";

    public void enqueueScan(Long scanId) {
        redisTemplate.opsForList().leftPush(SCAN_QUEUE, scanId);
        updateScanStatus(scanId, "QUEUED");
        log.info("Enqueued scan: {}", scanId);
    }

    public Long dequeueScan(long timeoutSeconds) {
        Object result = redisTemplate.opsForList().rightPop(SCAN_QUEUE, timeoutSeconds, TimeUnit.SECONDS);
        if (result != null) {
            Long scanId = Long.valueOf(result.toString());
            updateScanStatus(scanId, "PROCESSING");
            log.info("Dequeued scan: {}", scanId);
            return scanId;
        }
        return null;
    }

    public long getQueueSize() {
        Long size = redisTemplate.opsForList().size(SCAN_QUEUE);
        return size != null ? size : 0;
    }

    public void updateScanStatus(Long scanId, String status) {
        String key = SCAN_STATUS_PREFIX + scanId;
        redisTemplate.opsForValue().set(key, status, 24, TimeUnit.HOURS);
    }

    public String getScanStatus(Long scanId) {
        String key = SCAN_STATUS_PREFIX + scanId;
        Object status = redisTemplate.opsForValue().get(key);
        return status != null ? status.toString() : "UNKNOWN";
    }

    public void updateScanProgress(Long scanId, int progress, String currentStep) {
        String key = SCAN_STATUS_PREFIX + scanId + ":progress";
        String value = progress + ":" + currentStep;
        redisTemplate.opsForValue().set(key, value, 24, TimeUnit.HOURS);
    }

    public ScanProgress getScanProgress(Long scanId) {
        String key = SCAN_STATUS_PREFIX + scanId + ":progress";
        Object value = redisTemplate.opsForValue().get(key);
        
        if (value != null) {
            String[] parts = value.toString().split(":", 2);
            if (parts.length == 2) {
                return new ScanProgress(Integer.parseInt(parts[0]), parts[1]);
            }
        }
        return new ScanProgress(0, "Initializing");
    }

    public record ScanProgress(int progress, String currentStep) {}
}
