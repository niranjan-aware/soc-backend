package com.omnigaurd.solilos.worker;

import com.omnigaurd.solilos.model.entity.Scan;
import com.omnigaurd.solilos.repository.ScanJpaRepo;
import com.omnigaurd.solilos.service.analysis.*;
import com.omnigaurd.solilos.service.scan.ScanQueueService;
import com.omnigaurd.solilos.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScanWorker {

    private final ScanQueueService scanQueueService;
    private final ScanJpaRepo scanJpaRepo;
    private final AnalysisOrchestrator analysisOrchestrator;
    private final DependencyAnalyzer dependencyAnalyzer;
    private final AIAnalyzer aiAnalyzer;
    private final SensitiveFileAnalyzer sensitiveFileAnalyzer;
    private final FileStorageService fileStorageService;

    @Async("scanExecutor")
    public void processScan(Long scanId) {
        log.info("Worker started processing scan: {}", scanId);
        
        Scan scan = scanJpaRepo.findById(scanId).orElse(null);
        if (scan == null) {
            log.error("Scan not found: {}", scanId);
            return;
        }

        try {
            scan.setStatus(Scan.ScanStatus.IN_PROGRESS);
            scanJpaRepo.save(scan);
            
            String scanPath = fileStorageService.getStoragePath(scanId);

            // Step 1: Tree-sitter + Semgrep (25%)
            scanQueueService.updateScanProgress(scanId, 10, "Running static analysis");
            log.info("[{}] Running Tree-sitter and Semgrep analysis", scanId);
            analysisOrchestrator.analyze(scanId);
            scanQueueService.updateScanProgress(scanId, 25, "Static analysis complete");

            // Step 2: Sensitive file detection (25%)
            scanQueueService.updateScanProgress(scanId, 30, "Checking for sensitive files");
            log.info("[{}] Running sensitive file analysis", scanId);
            var sensitiveResult = sensitiveFileAnalyzer.analyze(scanPath, scanId);
            if (sensitiveResult.isSuccess() && !sensitiveResult.getIssues().isEmpty()) {
                analysisOrchestrator.saveAnalysisResults(scan, sensitiveResult, scanPath);
            }
            scanQueueService.updateScanProgress(scanId, 50, "Sensitive file check complete");

            // Step 3: Dependency analysis (20%)
            scanQueueService.updateScanProgress(scanId, 60, "Analyzing dependencies");
            log.info("[{}] Running dependency analysis", scanId);
            DependencyAnalyzer.DependencyAnalysisResult depResult = 
                dependencyAnalyzer.analyze(scanPath, scan);
            scanQueueService.updateScanProgress(scanId, 80, "Dependency analysis complete");

            // Step 4: AI analysis (20%)
            scanQueueService.updateScanProgress(scanId, 85, "Running AI analysis");
            log.info("[{}] Running AI analysis", scanId);
            var aiResult = aiAnalyzer.analyze(scanPath, scanId);
            if (aiResult.isSuccess() && !aiResult.getIssues().isEmpty()) {
                analysisOrchestrator.saveAnalysisResults(scan, aiResult, scanPath);
            }
            scanQueueService.updateScanProgress(scanId, 100, "Analysis complete");

            // Mark as completed
            scan.setStatus(Scan.ScanStatus.COMPLETED);
            scan.setCompletedAt(LocalDateTime.now());
            scan.setScanDurationMs(
                java.time.Duration.between(scan.getStartedAt(), scan.getCompletedAt()).toMillis()
            );
            scanJpaRepo.save(scan);

            scanQueueService.updateScanStatus(scanId, "COMPLETED");
            log.info("[{}] Scan completed successfully", scanId);

        } catch (Exception e) {
            log.error("[{}] Scan failed", scanId, e);
            scan.setStatus(Scan.ScanStatus.FAILED);
            scan.setErrorMessage("Analysis failed: " + e.getMessage());
            scan.setCompletedAt(LocalDateTime.now());
            scanJpaRepo.save(scan);
            
            scanQueueService.updateScanStatus(scanId, "FAILED");
        }
    }
}
