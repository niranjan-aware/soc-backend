package com.omnigaurd.solilos.service.analysis;

import com.omnigaurd.solilos.model.dto.AnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
@Slf4j
public class TreeSitterAnalyzer {

    // Simplified pattern-based analysis (can be enhanced with actual Tree-sitter later)
    private static final List<SecurityPattern> SECURITY_PATTERNS = List.of(
        new SecurityPattern("eval\\(", "CODE_INJECTION", "Use of eval() - potential code injection", "HIGH"),
        new SecurityPattern("exec\\(", "CODE_INJECTION", "Use of exec() - potential code injection", "HIGH"),
        new SecurityPattern("Runtime\\.getRuntime\\(\\)\\.exec", "COMMAND_INJECTION", "Runtime.exec() - potential command injection", "CRITICAL"),
        new SecurityPattern("SQLException", "SQL_ERROR_HANDLING", "SQL exception handling", "MEDIUM"),
        new SecurityPattern("password\\s*=\\s*[\"'][^\"']+[\"']", "HARDCODED_PASSWORD", "Hardcoded password detected", "CRITICAL"),
        new SecurityPattern("api[_-]?key\\s*=\\s*[\"'][^\"']+[\"']", "HARDCODED_SECRET", "Hardcoded API key detected", "CRITICAL"),
        new SecurityPattern("TODO|FIXME|HACK", "CODE_QUALITY", "Code quality marker found", "LOW"),
        new SecurityPattern("System\\.out\\.print", "DEBUG_CODE", "Debug print statement", "INFO")
    );

    public AnalysisResult analyze(String scanPath, Long scanId) {
        AnalysisResult result = new AnalysisResult();
        result.setScanId(scanId);
        result.setAnalyzerSource("TREE_SITTER");
        result.setIssues(new ArrayList<>());

        try {
            log.info("Starting Tree-sitter pattern analysis for scan: {}", scanId);
            
            Path root = Paths.get(scanPath);
            
            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                    .filter(path -> isSourceFile(path.toString()))
                    .filter(path -> !path.toString().contains("/.git/"))
                    .forEach(path -> analyzeFile(path, scanPath, result));
            }
            
            result.setSuccess(true);
            result.setTotalIssues(result.getIssues().size());
            log.info("Tree-sitter analysis found {} issues", result.getTotalIssues());
            
        } catch (Exception e) {
            log.error("Tree-sitter analysis failed for scan: {}", scanId, e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    private void analyzeFile(Path filePath, String scanPath, AnalysisResult result) {
        try {
            String content = Files.readString(filePath);
            String[] lines = content.split("\n");
            
            String relativePath = filePath.toString().replace(scanPath + "/", "");
            
            for (SecurityPattern pattern : SECURITY_PATTERNS) {
                Pattern regex = Pattern.compile(pattern.pattern, Pattern.CASE_INSENSITIVE);
                
                for (int i = 0; i < lines.length; i++) {
                    Matcher matcher = regex.matcher(lines[i]);
                    if (matcher.find()) {
                        AnalysisResult.IssueDetail issue = new AnalysisResult.IssueDetail();
                        issue.setFilePath(relativePath);
                        issue.setIssueType(pattern.issueType);
                        issue.setSeverity(pattern.severity);
                        issue.setRuleId("PATTERN_" + pattern.issueType);
                        issue.setRuleName(pattern.description);
                        issue.setDescription(pattern.description);
                        issue.setLineNumber(i + 1);
                        issue.setCodeSnippet(lines[i].trim());
                        
                        result.getIssues().add(issue);
                        incrementSeverityCount(result, pattern.severity);
                    }
                }
            }
            
        } catch (IOException e) {
            log.warn("Failed to analyze file: {}", filePath, e);
        }
    }

    private boolean isSourceFile(String path) {
        String lowerPath = path.toLowerCase();
        return lowerPath.endsWith(".java") || 
               lowerPath.endsWith(".py") || 
               lowerPath.endsWith(".js") || 
               lowerPath.endsWith(".ts") ||
               lowerPath.endsWith(".jsx") ||
               lowerPath.endsWith(".tsx") ||
               lowerPath.endsWith(".go") ||
               lowerPath.endsWith(".rs") ||
               lowerPath.endsWith(".cpp") ||
               lowerPath.endsWith(".c") ||
               lowerPath.endsWith(".php");
    }

    private void incrementSeverityCount(AnalysisResult result, String severity) {
        if (severity == null) return;
        
        switch (severity.toUpperCase()) {
            case "CRITICAL" -> result.setCriticalCount(result.getCriticalCount() + 1);
            case "HIGH" -> result.setHighCount(result.getHighCount() + 1);
            case "MEDIUM" -> result.setMediumCount(result.getMediumCount() + 1);
            case "LOW", "INFO" -> result.setLowCount(result.getLowCount() + 1);
        }
    }

    private record SecurityPattern(String pattern, String issueType, String description, String severity) {}
}
