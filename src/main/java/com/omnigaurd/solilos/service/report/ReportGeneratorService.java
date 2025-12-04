package com.omnigaurd.solilos.service.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnigaurd.solilos.model.dto.IssueResponse;
import com.omnigaurd.solilos.model.dto.ScanResponse;
import com.omnigaurd.solilos.model.entity.Dependency;
import com.omnigaurd.solilos.model.entity.Issue;
import com.omnigaurd.solilos.model.entity.Scan;
import com.omnigaurd.solilos.repository.DependencyJpaRepo;
import com.omnigaurd.solilos.repository.IssueJpaRepo;
import com.omnigaurd.solilos.repository.ScanJpaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportGeneratorService {

    private final ScanJpaRepo scanJpaRepo;
    private final IssueJpaRepo issueJpaRepo;
    private final DependencyJpaRepo dependencyJpaRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public File generateJsonReport(Long scanId) throws IOException {
        Scan scan = scanJpaRepo.findById(scanId)
            .orElseThrow(() -> new RuntimeException("Scan not found"));

        Map<String, Object> report = new HashMap<>();
        report.put("scanId", scan.getId());
        report.put("repositoryName", scan.getRepository().getRepoName());
        report.put("scanStatus", scan.getStatus().name());
        report.put("startedAt", scan.getStartedAt().toString());
        report.put("completedAt", scan.getCompletedAt() != null ? scan.getCompletedAt().toString() : null);
        report.put("totalFiles", scan.getTotalFiles());
        report.put("totalIssues", scan.getTotalIssues());
        
        Map<String, Integer> severityCounts = new HashMap<>();
        severityCounts.put("critical", scan.getCriticalCount());
        severityCounts.put("high", scan.getHighCount());
        severityCounts.put("medium", scan.getMediumCount());
        severityCounts.put("low", scan.getLowCount());
        report.put("severityCounts", severityCounts);

        List<Issue> issues = issueJpaRepo.findByScanId(scanId);
        report.put("issues", issues.stream().map(IssueResponse::from).collect(Collectors.toList()));

        List<Dependency> dependencies = dependencyJpaRepo.findByScanIdAndIsVulnerable(scanId, true);
        report.put("vulnerableDependencies", dependencies);

        File reportFile = File.createTempFile("scan-report-" + scanId + "-", ".json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile, report);
        
        log.info("Generated JSON report: {}", reportFile.getAbsolutePath());
        return reportFile;
    }

    public File generateCsvReport(Long scanId) throws IOException {
        Scan scan = scanJpaRepo.findById(scanId)
            .orElseThrow(() -> new RuntimeException("Scan not found"));

        List<Issue> issues = issueJpaRepo.findByScanId(scanId);

        File reportFile = File.createTempFile("scan-report-" + scanId + "-", ".csv");
        
        try (FileWriter writer = new FileWriter(reportFile)) {
            // Header
            writer.write("File Path,Severity,Issue Type,Rule ID,Description,Line Number,Analyzer\n");
            
            // Data
            for (Issue issue : issues) {
                writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,\"%s\"\n",
                    issue.getFile().getFilePath(),
                    issue.getSeverity().name(),
                    issue.getIssueType().name(),
                    issue.getRuleId(),
                    issue.getDescription().replace("\"", "\"\""),
                    issue.getLineNumber() != null ? issue.getLineNumber() : 0,
                    issue.getAnalyzerSource().name()
                ));
            }
        }

        log.info("Generated CSV report: {}", reportFile.getAbsolutePath());
        return reportFile;
    }

    public File generateTextReport(Long scanId) throws IOException {
        Scan scan = scanJpaRepo.findById(scanId)
            .orElseThrow(() -> new RuntimeException("Scan not found"));

        List<Issue> issues = issueJpaRepo.findByScanId(scanId);
        List<Dependency> vulnDeps = dependencyJpaRepo.findByScanIdAndIsVulnerable(scanId, true);

        File reportFile = File.createTempFile("scan-report-" + scanId + "-", ".txt");
        
        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write("═══════════════════════════════════════════════════════════\n");
            writer.write("                 SECURITY SCAN REPORT\n");
            writer.write("═══════════════════════════════════════════════════════════\n\n");
            
            writer.write(String.format("Repository: %s\n", scan.getRepository().getRepoName()));
            writer.write(String.format("Scan ID: %d\n", scan.getId()));
            writer.write(String.format("Status: %s\n", scan.getStatus().name()));
            writer.write(String.format("Started: %s\n", scan.getStartedAt()));
            if (scan.getCompletedAt() != null) {
                writer.write(String.format("Completed: %s\n", scan.getCompletedAt()));
            }
            writer.write(String.format("Duration: %d ms\n\n", scan.getScanDurationMs()));

            writer.write("───────────────────────────────────────────────────────────\n");
            writer.write("SUMMARY\n");
            writer.write("───────────────────────────────────────────────────────────\n\n");
            writer.write(String.format("Total Files Scanned: %d\n", scan.getTotalFiles()));
            writer.write(String.format("Total Issues Found: %d\n\n", scan.getTotalIssues()));
            
            writer.write("Issues by Severity:\n");
            writer.write(String.format("  CRITICAL: %d\n", scan.getCriticalCount()));
            writer.write(String.format("  HIGH:     %d\n", scan.getHighCount()));
            writer.write(String.format("  MEDIUM:   %d\n", scan.getMediumCount()));
            writer.write(String.format("  LOW:      %d\n\n", scan.getLowCount()));

            if (!vulnDeps.isEmpty()) {
                writer.write(String.format("Vulnerable Dependencies: %d\n\n", vulnDeps.size()));
            }

            writer.write("───────────────────────────────────────────────────────────\n");
            writer.write("DETAILED ISSUES\n");
            writer.write("───────────────────────────────────────────────────────────\n\n");

            for (Issue issue : issues) {
                writer.write(String.format("[%s] %s\n", issue.getSeverity().name(), issue.getRuleName()));
                writer.write(String.format("File: %s:%d\n", 
                    issue.getFile().getFilePath(), 
                    issue.getLineNumber() != null ? issue.getLineNumber() : 0));
                writer.write(String.format("Description: %s\n", issue.getDescription()));
                writer.write(String.format("Analyzer: %s\n", issue.getAnalyzerSource().name()));
                if (issue.getCweId() != null) {
                    writer.write(String.format("CWE: %s\n", issue.getCweId()));
                }
                writer.write("\n");
            }

            if (!vulnDeps.isEmpty()) {
                writer.write("───────────────────────────────────────────────────────────\n");
                writer.write("VULNERABLE DEPENDENCIES\n");
                writer.write("───────────────────────────────────────────────────────────\n\n");

                for (Dependency dep : vulnDeps) {
                    writer.write(String.format("[%s] %s\n", 
                        dep.getSeverity() != null ? dep.getSeverity().name() : "UNKNOWN",
                        dep.getPackageName()));
                    writer.write(String.format("Current Version: %s\n", dep.getCurrentVersion()));
                    writer.write(String.format("Vulnerability: %s\n", dep.getVulnerabilityId()));
                    writer.write(String.format("Fix Version: %s\n", dep.getFixVersion()));
                    writer.write(String.format("Description: %s\n\n", dep.getDescription()));
                }
            }

            writer.write("═══════════════════════════════════════════════════════════\n");
            writer.write(String.format("Generated: %s\n", 
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            writer.write("═══════════════════════════════════════════════════════════\n");
        }

        log.info("Generated text report: {}", reportFile.getAbsolutePath());
        return reportFile;
    }
}
