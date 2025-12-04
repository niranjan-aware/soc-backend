package com.omnigaurd.solilos.service.analysis;

import com.omnigaurd.solilos.model.dto.AnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
@Slf4j
public class SensitiveFileAnalyzer {

    // Sensitive files to detect
    private static final Set<String> SENSITIVE_FILES = Set.of(
        ".env", ".env.local", ".env.production", ".env.development",
        "id_rsa", "id_dsa", "id_ecdsa", "id_ed25519",
        "config.json", "secrets.yml", "secrets.yaml",
        "credentials.json", "service-account.json",
        ".aws/credentials", ".ssh/id_rsa",
        "database.yml", "application.properties",
        ".git-credentials", ".netrc",
        "shadow", "passwd",
        "private.key", "privatekey.pem",
        "docker-compose.yml" // Check for exposed secrets
    );

    // Patterns for exposed credentials
    private static final List<CredentialPattern> CREDENTIAL_PATTERNS = List.of(
        new CredentialPattern("AWS Access Key", "AKIA[0-9A-Z]{16}", "CRITICAL"),
        new CredentialPattern("AWS Secret Key", "aws_secret_access_key\\s*=\\s*['\"]?([A-Za-z0-9/+=]{40})['\"]?", "CRITICAL"),
        new CredentialPattern("GitHub Token", "gh[pousr]_[A-Za-z0-9]{36}", "CRITICAL"),
        new CredentialPattern("Generic API Key", "api[_-]?key\\s*[=:]\\s*['\"]?([A-Za-z0-9]{20,})['\"]?", "HIGH"),
        new CredentialPattern("Generic Secret", "secret\\s*[=:]\\s*['\"]?([A-Za-z0-9]{20,})['\"]?", "HIGH"),
        new CredentialPattern("Password in Code", "password\\s*[=:]\\s*['\"]([^'\"]{8,})['\"]", "HIGH"),
        new CredentialPattern("Private Key", "-----BEGIN (RSA|DSA|EC|OPENSSH) PRIVATE KEY-----", "CRITICAL"),
        new CredentialPattern("Database Connection", "mongodb://[^\\s]+", "HIGH"),
        new CredentialPattern("Database Password", "DB_PASSWORD\\s*=\\s*['\"]?([^'\"\\s]+)['\"]?", "HIGH"),
        new CredentialPattern("JWT Secret", "jwt[_-]?secret\\s*[=:]\\s*['\"]?([A-Za-z0-9]{20,})['\"]?", "HIGH"),
        new CredentialPattern("Stripe Key", "sk_live_[0-9a-zA-Z]{24}", "CRITICAL"),
        new CredentialPattern("Google API Key", "AIza[0-9A-Za-z\\-_]{35}", "HIGH"),
        new CredentialPattern("Slack Token", "xox[baprs]-[0-9]{10,12}-[0-9]{10,12}-[A-Za-z0-9]{24,32}", "HIGH"),
        new CredentialPattern("Generic Token", "token\\s*[=:]\\s*['\"]?([A-Za-z0-9]{32,})['\"]?", "MEDIUM")
    );

    public AnalysisResult analyze(String scanPath, Long scanId) {
        AnalysisResult result = new AnalysisResult();
        result.setScanId(scanId);
        result.setAnalyzerSource("SENSITIVE_FILE_DETECTOR");
        result.setIssues(new ArrayList<>());

        try {
            log.info("Starting sensitive file analysis for scan: {}", scanId);
            Path root = Paths.get(scanPath);

            // Check for sensitive files
            checkSensitiveFiles(root, result);

            // Check for exposed credentials in code
            checkExposedCredentials(root, scanPath, result);

            // Check for missing README
            checkMissingReadme(root, result);

            // Check for missing LICENSE
            checkMissingLicense(root, result);

            result.setSuccess(true);
            result.setTotalIssues(result.getIssues().size());
            log.info("Sensitive file analysis found {} issues", result.getTotalIssues());

        } catch (Exception e) {
            log.error("Sensitive file analysis failed", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    private void checkSensitiveFiles(Path root, AnalysisResult result) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                .filter(path -> !path.toString().contains("/.git/"))
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    String relativePath = root.relativize(path).toString();

                    // Check if file matches sensitive patterns
                    for (String sensitiveFile : SENSITIVE_FILES) {
                        if (fileName.equals(sensitiveFile) || relativePath.contains(sensitiveFile)) {
                            AnalysisResult.IssueDetail issue = new AnalysisResult.IssueDetail();
                            issue.setFilePath(relativePath);
                            issue.setIssueType("MISCONFIGURATION");
                            issue.setSeverity("CRITICAL");
                            issue.setRuleId("SENSITIVE_FILE");
                            issue.setRuleName("Sensitive File Detected");
                            issue.setDescription("Sensitive file '" + fileName + "' should not be committed to repository");
                            issue.setSuggestedFix("Add '" + fileName + "' to .gitignore and remove from repository history");
                            issue.setOwaspCategory("A01:2021-Broken Access Control");
                            issue.setLineNumber(1);
                            issue.setAnalyzerSource("SENSITIVE_FILE_DETECTOR");

                            result.getIssues().add(issue);
                            incrementSeverityCount(result, "CRITICAL");
                            break;
                        }
                    }
                });
        }
    }

    private void checkExposedCredentials(Path root, String scanPath, AnalysisResult result) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                .filter(path -> !path.toString().contains("/.git/"))
                .filter(path -> isTextFile(path.toString()))
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        String[] lines = content.split("\n");
                        String relativePath = root.relativize(path).toString();

                        for (int i = 0; i < lines.length; i++) {
                            String line = lines[i];

                            for (CredentialPattern pattern : CREDENTIAL_PATTERNS) {
                                Pattern regex = Pattern.compile(pattern.pattern, Pattern.CASE_INSENSITIVE);
                                Matcher matcher = regex.matcher(line);

                                if (matcher.find()) {
                                    AnalysisResult.IssueDetail issue = new AnalysisResult.IssueDetail();
                                    issue.setFilePath(relativePath);
                                    issue.setIssueType("SECURITY");
                                    issue.setSeverity(pattern.severity);
                                    issue.setRuleId("EXPOSED_CREDENTIAL");
                                    issue.setRuleName("Exposed " + pattern.name);
                                    issue.setDescription("Potential " + pattern.name.toLowerCase() + " found in code");
                                    issue.setLineNumber(i + 1);
                                    issue.setCodeSnippet(maskSensitiveData(line));
                                    issue.setSuggestedFix("Remove hardcoded credentials and use environment variables or secrets management");
                                    issue.setCweId("CWE-798");
                                    issue.setOwaspCategory("A02:2021-Cryptographic Failures");
                                    issue.setAnalyzerSource("SENSITIVE_FILE_DETECTOR");

                                    result.getIssues().add(issue);
                                    incrementSeverityCount(result, pattern.severity);
                                }
                            }
                        }
                    } catch (IOException e) {
                        log.warn("Failed to read file: {}", path, e);
                    }
                });
        }
    }

    private void checkMissingReadme(Path root, AnalysisResult result) {
        boolean hasReadme = false;
        try (Stream<Path> paths = Files.list(root)) {
            hasReadme = paths.anyMatch(path -> {
                String fileName = path.getFileName().toString().toLowerCase();
                return fileName.equals("readme.md") || 
                       fileName.equals("readme") || 
                       fileName.equals("readme.txt");
            });
        } catch (IOException e) {
            log.warn("Failed to check for README", e);
        }

        if (!hasReadme) {
            AnalysisResult.IssueDetail issue = new AnalysisResult.IssueDetail();
            issue.setFilePath(".");
            issue.setIssueType("CODE_QUALITY");
            issue.setSeverity("LOW");
            issue.setRuleId("MISSING_README");
            issue.setRuleName("Missing README File");
            issue.setDescription("Repository should have a README file documenting the project");
            issue.setSuggestedFix("Add a README.md file with project description, setup instructions, and usage examples");
            issue.setLineNumber(1);
            issue.setAnalyzerSource("SENSITIVE_FILE_DETECTOR");

            result.getIssues().add(issue);
            incrementSeverityCount(result, "LOW");
        }
    }

    private void checkMissingLicense(Path root, AnalysisResult result) {
        boolean hasLicense = false;
        try (Stream<Path> paths = Files.list(root)) {
            hasLicense = paths.anyMatch(path -> {
                String fileName = path.getFileName().toString().toLowerCase();
                return fileName.equals("license") || 
                       fileName.equals("license.md") || 
                       fileName.equals("license.txt") ||
                       fileName.equals("licence");
            });
        } catch (IOException e) {
            log.warn("Failed to check for LICENSE", e);
        }

        if (!hasLicense) {
            AnalysisResult.IssueDetail issue = new AnalysisResult.IssueDetail();
            issue.setFilePath(".");
            issue.setIssueType("CODE_QUALITY");
            issue.setSeverity("MEDIUM");
            issue.setRuleId("MISSING_LICENSE");
            issue.setRuleName("Missing LICENSE File");
            issue.setDescription("Repository should have a LICENSE file specifying usage terms");
            issue.setSuggestedFix("Add a LICENSE file (e.g., MIT, Apache 2.0, GPL) to clarify usage rights");
            issue.setLineNumber(1);
            issue.setAnalyzerSource("SENSITIVE_FILE_DETECTOR");

            result.getIssues().add(issue);
            incrementSeverityCount(result, "MEDIUM");
        }
    }

    private boolean isTextFile(String path) {
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
               lowerPath.endsWith(".php") ||
               lowerPath.endsWith(".yml") ||
               lowerPath.endsWith(".yaml") ||
               lowerPath.endsWith(".json") ||
               lowerPath.endsWith(".xml") ||
               lowerPath.endsWith(".properties") ||
               lowerPath.endsWith(".env") ||
               lowerPath.endsWith(".sh") ||
               lowerPath.endsWith(".bash");
    }

    private String maskSensitiveData(String line) {
        // Mask potential secrets in the snippet
        return line.replaceAll("(['\"])[A-Za-z0-9/+=]{20,}(['\"])", "$1***REDACTED***$2");
    }

    private void incrementSeverityCount(AnalysisResult result, String severity) {
        switch (severity.toUpperCase()) {
            case "CRITICAL" -> result.setCriticalCount(result.getCriticalCount() + 1);
            case "HIGH" -> result.setHighCount(result.getHighCount() + 1);
            case "MEDIUM" -> result.setMediumCount(result.getMediumCount() + 1);
            case "LOW" -> result.setLowCount(result.getLowCount() + 1);
        }
    }

    private record CredentialPattern(String name, String pattern, String severity) {}
}
