package com.omnigaurd.solilos.service.storage;

import com.omnigaurd.solilos.config.StorageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final StorageConfig storageConfig;

    public String getStoragePath(Long scanId) {
        return Paths.get(storageConfig.getBasePath(), scanId.toString()).toString();
    }

    public boolean createScanDirectory(Long scanId) {
        try {
            Path scanPath = Paths.get(getStoragePath(scanId));
            Files.createDirectories(scanPath);
            log.info("Created scan directory: {}", scanPath);
            return true;
        } catch (IOException e) {
            log.error("Failed to create scan directory for scanId: {}", scanId, e);
            return false;
        }
    }

    public boolean deleteScanDirectory(Long scanId) {
        try {
            Path scanPath = Paths.get(getStoragePath(scanId));
            if (Files.exists(scanPath)) {
                FileUtils.deleteDirectory(scanPath.toFile());
                log.info("Deleted scan directory: {}", scanPath);
                return true;
            }
            return false;
        } catch (IOException e) {
            log.error("Failed to delete scan directory for scanId: {}", scanId, e);
            return false;
        }
    }

    public long getDirectorySize(Long scanId) {
        try {
            Path scanPath = Paths.get(getStoragePath(scanId));
            if (!Files.exists(scanPath)) {
                return 0;
            }
            return FileUtils.sizeOfDirectory(scanPath.toFile());
        } catch (Exception e) {
            log.error("Failed to get directory size for scanId: {}", scanId, e);
            return 0;
        }
    }

    public void cleanupOldScans() {
        try {
            Path basePath = Paths.get(storageConfig.getBasePath());
            if (!Files.exists(basePath)) {
                return;
            }

            LocalDateTime cutoffTime = LocalDateTime.now()
                .minus(storageConfig.getCleanupAfterHours(), ChronoUnit.HOURS);

            try (Stream<Path> paths = Files.list(basePath)) {
                paths.filter(Files::isDirectory)
                    .forEach(path -> {
                        try {
                            LocalDateTime lastModified = LocalDateTime.ofInstant(
                                Files.getLastModifiedTime(path).toInstant(),
                                java.time.ZoneId.systemDefault()
                            );
                            
                            if (lastModified.isBefore(cutoffTime)) {
                                FileUtils.deleteDirectory(path.toFile());
                                log.info("Cleaned up old scan directory: {}", path);
                            }
                        } catch (IOException e) {
                            log.error("Failed to cleanup directory: {}", path, e);
                        }
                    });
            }
        } catch (Exception e) {
            log.error("Failed to cleanup old scans", e);
        }
    }

    public String readFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + filePath);
        }
        return Files.readString(path);
    }

    public boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }
}
