package com.omnigaurd.solilos.service.analysis;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.omnigaurd.solilos.model.dto.AnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SemgrepAnalyzer {

    @Value("${semgrep.binary-path:/usr/local/bin/semgrep}")
    private String semgrepPath;

    private final Gson gson = new Gson();

    public AnalysisResult analyze(String scanPath, Long scanId) {
        AnalysisResult result = new AnalysisResult();
        result.setScanId(scanId);
        result.setAnalyzerSource("SEMGREP");
        result.setIssues(new ArrayList<>());

        try {
            log.info("Starting Semgrep analysis for scan: {} at path: {}", scanId, scanPath);

            // Check if semgrep is installed
            if (!isSemgrepInstalled()) {
                result.setSuccess(false);
                result.setErrorMessage("Semgrep not installed. Install with: pip3 install semgrep");
                log.error("Semgrep not found at: {}", semgrepPath);
                return result;
            }

            // Run Semgrep
            String outputPath = scanPath + "/semgrep-results.json";
            ProcessBuilder processBuilder = new ProcessBuilder(
                semgrepPath,
                "--config", "auto",  // Use auto config for common vulnerabilities
                "--json",
                "--output", outputPath,
                scanPath
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Read output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("Semgrep: {}", line);
            }

            int exitCode = process.waitFor();
            log.info("Semgrep completed with exit code: {}", exitCode);

            // Parse results
            if (Files.exists(Paths.get(outputPath))) {
                parseResults(outputPath, result, scanPath);
                result.setSuccess(true);
            } else {
                result.setSuccess(true); // No issues found
                log.info("No security issues found by Semgrep");
            }

        } catch (Exception e) {
            log.error("Semgrep analysis failed for scan: {}", scanId, e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    private boolean isSemgrepInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", "semgrep");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void parseResults(String outputPath, AnalysisResult result, String scanPath) {
        try {
            String jsonContent = Files.readString(Paths.get(outputPath));
            JsonObject root = gson.fromJson(jsonContent, JsonObject.class);
            
            if (root.has("results")) {
                JsonArray results = root.getAsJsonArray("results");
                
                for (int i = 0; i < results.size(); i++) {
                    JsonObject finding = results.get(i).getAsJsonObject();
                    
                    AnalysisResult.IssueDetail issue = new AnalysisResult.IssueDetail();
                    
                    // Extract file path
                    String fullPath = finding.get("path").getAsString();
                    issue.setFilePath(fullPath.replace(scanPath + File.separator, ""));
                    
                    // Extract check/rule info
                    if (finding.has("check_id")) {
                        issue.setRuleId(finding.get("check_id").getAsString());
                    }
                    
                    if (finding.has("extra")) {
                        JsonObject extra = finding.getAsJsonObject("extra");
                        
                        if (extra.has("message")) {
                            issue.setDescription(extra.get("message").getAsString());
                        }
                        
                        if (extra.has("severity")) {
                            String severity = extra.get("severity").getAsString().toUpperCase();
                            issue.setSeverity(mapSeverity(severity));
                        }
                        
                        if (extra.has("metadata")) {
                            JsonObject metadata = extra.getAsJsonObject("metadata");
                            if (metadata.has("cwe")) {
                                issue.setCweId(metadata.get("cwe").toString());
                            }
                            if (metadata.has("owasp")) {
                                issue.setOwaspCategory(metadata.get("owasp").toString());
                            }
                        }
                    }
                    
                    // Extract location
                    if (finding.has("start")) {
                        JsonObject start = finding.getAsJsonObject("start");
                        issue.setLineNumber(start.get("line").getAsInt());
                        issue.setColumnNumber(start.get("col").getAsInt());
                    }
                    
                    // Extract code snippet
                    if (finding.has("extra") && finding.getAsJsonObject("extra").has("lines")) {
                        issue.setCodeSnippet(finding.getAsJsonObject("extra").get("lines").getAsString());
                    }
                    
                    issue.setIssueType("SECURITY");
                    issue.setRuleName(issue.getRuleId());
                    
                    result.getIssues().add(issue);
                    
                    // Update counts
                    incrementSeverityCount(result, issue.getSeverity());
                }
                
                result.setTotalIssues(results.size());
                log.info("Parsed {} issues from Semgrep results", results.size());
            }
            
        } catch (Exception e) {
            log.error("Failed to parse Semgrep results", e);
        }
    }

    private String mapSeverity(String semgrepSeverity) {
        return switch (semgrepSeverity.toUpperCase()) {
            case "ERROR" -> "CRITICAL";
            case "WARNING" -> "HIGH";
            case "INFO" -> "MEDIUM";
            default -> "LOW";
        };
    }

    private void incrementSeverityCount(AnalysisResult result, String severity) {
        if (severity == null) return;
        
        switch (severity.toUpperCase()) {
            case "CRITICAL" -> result.setCriticalCount(result.getCriticalCount() + 1);
            case "HIGH" -> result.setHighCount(result.getHighCount() + 1);
            case "MEDIUM" -> result.setMediumCount(result.getMediumCount() + 1);
            case "LOW" -> result.setLowCount(result.getLowCount() + 1);
        }
    }
}
