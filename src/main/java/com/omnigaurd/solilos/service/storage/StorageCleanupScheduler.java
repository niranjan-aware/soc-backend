package com.omnigaurd.solilos.service.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageCleanupScheduler {

    private final FileStorageService fileStorageService;

    // Run cleanup every 6 hours
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000)
    public void cleanupOldScans() {
        log.info("Starting scheduled cleanup of old scan directories");
        fileStorageService.cleanupOldScans();
        log.info("Completed scheduled cleanup");
    }
}
