package com.omnigaurd.solilos.service.analysis;

import com.omnigaurd.solilos.model.dto.AnalysisResult;
import com.omnigaurd.solilos.service.GroqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIAnalyzer {

    private final GroqService groqService;

    private static final int MAX_FILE_SIZE = 50000; // 50KB max per file
    private static final int MAX_FILES_TO_ANALYZE = 10; // Analyze top 10 files

    public AnalysisResult analyze(String scanPath, Long scanId) {
        AnalysisResult result = new AnalysisResult();
        result.setScanId(scanId);
        result.setAnalyzerSource("AI");
        result.setIssues(new ArrayList<>());

        try {
            log.info("Starting AI analysis for scan: {}", scanId);

            List<Path> filesToAnalyze = selectFilesForAnalysis(scanPath);
            
            if (filesToAnalyze.isEmpty()) {
                log.info("No suitable files found for AI analysis");
                result.setSuccess(true);
                return result;
            }

            log.info("Selected {} files for AI analysis", filesToAnalyze.size());

            for (Path filePath : filesToAnalyze) {
                try {
                    analyzeFile(filePath, scanPath, result);
                } catch (Exception e) {
                    log.error("Failed to analyze file with AI: {}", filePath, e);
                }
            }

            result.setSuccess(true);
            result.setTotalIssues(result.getIssues().size());
            log.info("AI analysis completed. Found {} issues", result.getTotalIssues());

        } catch (Exception e) {
            log.error("AI analysis failed for scan: {}", scanId, e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    private List<Path> selectFilesForAnalysis(String scanPath) throws IOException {
        List<Path> selectedFiles = new ArrayList<>();
        Path root = Paths.get(scanPath);

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                .filter(path -> !path.toString().contains("/.git/"))
                .filter(path -> isSourceFile(path.toString()))
                .filter(path -> {
                    try {
                        return Files.size(path) < MAX_FILE_SIZE;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .limit(MAX_FILES_TO_ANALYZE)
                .forEach(selectedFiles::add);
        }

        return selectedFiles;
    }

    private void analyzeFile(Path filePath, String scanPath, AnalysisResult result) throws IOException {
        String content = Files.readString(filePath);
        String relativePath = filePath.toString().replace(scanPath + "/", "");

        String prompt = buildAnalysisPrompt(relativePath, content);

        try {
            String aiResponse = groqService.chat(prompt);
            parseAIResponse(aiResponse, relativePath, result);
        } catch (Exception e) {
            log.error("AI API call failed for file: {}", relativePath, e);
        }
    }

    private String buildAnalysisPrompt(String filePath, String content) {
        return String.format("""
            You are a security expert analyzing code for vulnerabilities.
            
            File: %s
            
            Code:
```
            %s
```
            
            Analyze this code and identify security vulnerabilities, code quality issues, and best practice violations.
            
            For each issue found, respond in this exact format:
            ISSUE_START
            LINE: <line_number>
            SEVERITY: <CRITICAL|HIGH|MEDIUM|LOW>
            TYPE: <issue_type>
            DESCRIPTION: <brief description>
            ISSUE_END
            
            If no issues found, respond with: NO_ISSUES_FOUND
            """, filePath, truncateContent(content, 4000));
    }

    private String truncateContent(String content, int maxChars) {
        if (content.length() <= maxChars) {
            return content;
        }
        return content.substring(0, maxChars) + "\n... (truncated)";
    }

    private void parseAIResponse(String aiResponse, String filePath, AnalysisResult result) {
        if (aiResponse.contains("NO_ISSUES_FOUND")) {
            return;
        }

        String[] issues = aiResponse.split("ISSUE_START");
        
        for (String issueBlock : issues) {
            if (!issueBlock.contains("ISSUE_END")) {
                continue;
            }

            try {
                AnalysisResult.IssueDetail issue = new AnalysisResult.IssueDetail();
                issue.setFilePath(filePath);
                issue.setIssueType("CODE_QUALITY");
                issue.setAnalyzerSource("AI");

                // Extract line number
                String lineStr = extractValue(issueBlock, "LINE:");
                if (lineStr != null) {
                    try {
                        issue.setLineNumber(Integer.parseInt(lineStr.trim()));
                    } catch (NumberFormatException e) {
                        issue.setLineNumber(1);
                    }
                }

                // Extract severity
                String severity = extractValue(issueBlock, "SEVERITY:");
                issue.setSeverity(severity != null ? severity.trim() : "MEDIUM");

                // Extract type
                String type = extractValue(issueBlock, "TYPE:");
                if (type != null) {
                    issue.setRuleName(type.trim());
                }

                // Extract description
                String description = extractValue(issueBlock, "DESCRIPTION:");
                issue.setDescription(description != null ? description.trim() : "AI detected potential issue");

                issue.setRuleId("AI_CHECK");

                result.getIssues().add(issue);
                incrementSeverityCount(result, issue.getSeverity());

            } catch (Exception e) {
                log.warn("Failed to parse AI issue block: {}", issueBlock, e);
            }
        }
    }

    private String extractValue(String text, String key) {
        int startIdx = text.indexOf(key);
        if (startIdx == -1) {
            return null;
        }
        startIdx += key.length();
        int endIdx = text.indexOf("\n", startIdx);
        if (endIdx == -1) {
            endIdx = text.length();
        }
        return text.substring(startIdx, endIdx);
    }

    private boolean isSourceFile(String path) {
        String lowerPath = path.toLowerCase();
        return lowerPath.endsWith(".java") || 
               lowerPath.endsWith(".py") || 
               lowerPath.endsWith(".js") || 
               lowerPath.endsWith(".ts") ||
               lowerPath.endsWith(".jsx") ||
               lowerPath.endsWith(".tsx");
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
