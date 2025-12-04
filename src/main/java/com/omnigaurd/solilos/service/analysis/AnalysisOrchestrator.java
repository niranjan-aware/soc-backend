package com.omnigaurd.solilos.service.analysis;

import com.omnigaurd.solilos.model.dto.AnalysisResult;
import com.omnigaurd.solilos.model.entity.File;
import com.omnigaurd.solilos.model.entity.Issue;
import com.omnigaurd.solilos.model.entity.Scan;
import com.omnigaurd.solilos.repository.FileJpaRepo;
import com.omnigaurd.solilos.repository.IssueJpaRepo;
import com.omnigaurd.solilos.repository.ScanJpaRepo;
import com.omnigaurd.solilos.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisOrchestrator {

    private final SemgrepAnalyzer semgrepAnalyzer;
    private final TreeSitterAnalyzer treeSitterAnalyzer;
    private final ScanJpaRepo scanJpaRepo;
    private final FileJpaRepo fileJpaRepo;
    private final IssueJpaRepo issueJpaRepo;
    private final FileStorageService fileStorageService;

    @Transactional
    public void analyze(Long scanId) {
        log.info("Starting analysis orchestration for scan: {}", scanId);
        
        Scan scan = scanJpaRepo.findById(scanId)
            .orElseThrow(() -> new RuntimeException("Scan not found: " + scanId));
        
        String scanPath = fileStorageService.getStoragePath(scanId);
        
        try {
            // Run Tree-sitter analysis
            log.info("Running Tree-sitter analysis...");
            AnalysisResult treeSitterResult = treeSitterAnalyzer.analyze(scanPath, scanId);
            saveAnalysisResults(scan, treeSitterResult, scanPath);
            
            // Run Semgrep analysis
            log.info("Running Semgrep analysis...");
            AnalysisResult semgrepResult = semgrepAnalyzer.analyze(scanPath, scanId);
            saveAnalysisResults(scan, semgrepResult, scanPath);
            
            // Update scan statistics
            updateScanStatistics(scan);
            
            log.info("Analysis completed for scan: {}. Total issues: {}", 
                scanId, scan.getTotalIssues());
            
        } catch (Exception e) {
            log.error("Analysis failed for scan: {}", scanId, e);
            scan.setErrorMessage("Analysis failed: " + e.getMessage());
            scanJpaRepo.save(scan);
        }
    }

    @Transactional
    public void saveAnalysisResults(Scan scan, AnalysisResult result, String scanPath) {
        if (!result.isSuccess() || result.getIssues().isEmpty()) {
            log.info("No issues found by {}", result.getAnalyzerSource());
            return;
        }
        
        Map<String, File> fileCache = new HashMap<>();
        
        for (AnalysisResult.IssueDetail issueDetail : result.getIssues()) {
            try {
                File file = fileCache.computeIfAbsent(issueDetail.getFilePath(), 
                    path -> getOrCreateFile(scan, path, scanPath));
                
                Issue issue = new Issue();
                issue.setScan(scan);
                issue.setFile(file);
                issue.setIssueType(Issue.IssueType.valueOf(issueDetail.getIssueType()));
                issue.setSeverity(Issue.Severity.valueOf(issueDetail.getSeverity()));
                issue.setRuleId(issueDetail.getRuleId());
                issue.setRuleName(issueDetail.getRuleName());
                issue.setDescription(issueDetail.getDescription());
                issue.setLineNumber(issueDetail.getLineNumber());
                issue.setColumnNumber(issueDetail.getColumnNumber());
                issue.setCodeSnippet(issueDetail.getCodeSnippet());
                issue.setSuggestedFix(issueDetail.getSuggestedFix());
                issue.setCweId(issueDetail.getCweId());
                issue.setOwaspCategory(issueDetail.getOwaspCategory());
                issue.setAnalyzerSource(Issue.AnalyzerSource.valueOf(result.getAnalyzerSource()));
                issue.setConfidence(BigDecimal.valueOf(0.8));
                issue.setFalsePositive(false);
                
                issueJpaRepo.save(issue);
                
            } catch (Exception e) {
                log.error("Failed to save issue: {}", issueDetail, e);
            }
        }
        
        updateScanStatistics(scan);
    }

    private File getOrCreateFile(Scan scan, String relativePath, String scanPath) {
        String fullPath = scanPath + "/" + relativePath;
        
        try {
            java.nio.file.Path path = Paths.get(fullPath);
            
            if (!Files.exists(path)) {
                log.warn("File not found: {}", fullPath);
                return createFileEntity(scan, relativePath, fullPath, 0, 0, "");
            }
            
            long fileSize = Files.size(path);
            long lineCount = Files.lines(path).count();
            String fileHash = calculateFileHash(path);
            
            return createFileEntity(scan, relativePath, fullPath, fileSize, (int) lineCount, fileHash);
            
        } catch (Exception e) {
            log.error("Error processing file: {}", fullPath, e);
            return createFileEntity(scan, relativePath, fullPath, 0, 0, "");
        }
    }

    private File createFileEntity(Scan scan, String relativePath, String fullPath, 
                                   long fileSize, int lineCount, String fileHash) {
        File file = new File();
        file.setScan(scan);
        file.setFilePath(relativePath);
        file.setLanguage(detectLanguage(relativePath));
        file.setLinesOfCode(lineCount);
        file.setFileSizeBytes(fileSize);
        file.setFileHash(fileHash);
        file.setStoragePath(fullPath);
        
        return fileJpaRepo.save(file);
    }

    private String detectLanguage(String filePath) {
        String extension = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "java" -> "Java";
            case "py" -> "Python";
            case "js" -> "JavaScript";
            case "ts" -> "TypeScript";
            case "jsx" -> "React";
            case "tsx" -> "React TypeScript";
            case "go" -> "Go";
            case "rs" -> "Rust";
            case "cpp", "cc" -> "C++";
            case "c" -> "C";
            case "php" -> "PHP";
            default -> "Unknown";
        };
    }

    private String calculateFileHash(java.nio.file.Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(path);
            byte[] hash = digest.digest(bytes);
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Failed to calculate file hash", e);
            return "";
        }
    }

    private void updateScanStatistics(Scan scan) {
        Long totalIssues = issueJpaRepo.countByScanIdAndSeverity(scan.getId(), null);
        Long critical = issueJpaRepo.countByScanIdAndSeverity(scan.getId(), Issue.Severity.CRITICAL);
        Long high = issueJpaRepo.countByScanIdAndSeverity(scan.getId(), Issue.Severity.HIGH);
        Long medium = issueJpaRepo.countByScanIdAndSeverity(scan.getId(), Issue.Severity.MEDIUM);
        Long low = issueJpaRepo.countByScanIdAndSeverity(scan.getId(), Issue.Severity.LOW);
        
        scan.setTotalIssues(totalIssues != null ? totalIssues.intValue() : 0);
        scan.setCriticalCount(critical != null ? critical.intValue() : 0);
        scan.setHighCount(high != null ? high.intValue() : 0);
        scan.setMediumCount(medium != null ? medium.intValue() : 0);
        scan.setLowCount(low != null ? low.intValue() : 0);
        
        scanJpaRepo.save(scan);
    }
}
