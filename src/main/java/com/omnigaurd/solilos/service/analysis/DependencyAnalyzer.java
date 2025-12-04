package com.omnigaurd.solilos.service.analysis;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.omnigaurd.solilos.model.entity.Dependency;
import com.omnigaurd.solilos.model.entity.Issue;
import com.omnigaurd.solilos.model.entity.Scan;
import com.omnigaurd.solilos.repository.DependencyJpaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class DependencyAnalyzer {

    private final DependencyJpaRepo dependencyJpaRepo;
    private final Gson gson = new Gson();

    public DependencyAnalysisResult analyze(String scanPath, Scan scan) {
        DependencyAnalysisResult result = new DependencyAnalysisResult();
        result.setTotalDependencies(0);
        result.setVulnerableDependencies(0);

        try {
            log.info("Starting dependency analysis for scan: {}", scan.getId());

            // Analyze Maven dependencies (pom.xml)
            analyzeMavenDependencies(scanPath, scan, result);

            // Analyze NPM dependencies (package.json)
            analyzeNpmDependencies(scanPath, scan, result);

            // Analyze Python dependencies (requirements.txt)
            analyzePythonDependencies(scanPath, scan, result);

            log.info("Dependency analysis completed. Total: {}, Vulnerable: {}", 
                result.getTotalDependencies(), result.getVulnerableDependencies());

        } catch (Exception e) {
            log.error("Dependency analysis failed", e);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    private void analyzeMavenDependencies(String scanPath, Scan scan, DependencyAnalysisResult result) {
        try {
            Path pomPath = Paths.get(scanPath, "pom.xml");
            if (!Files.exists(pomPath)) {
                return;
            }

            log.info("Found pom.xml, analyzing Maven dependencies");
            String content = Files.readString(pomPath);

            // Extract dependencies using regex
            Pattern dependencyPattern = Pattern.compile(
                "<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>\\s*<version>([^<]+)</version>",
                Pattern.DOTALL
            );
            Matcher matcher = dependencyPattern.matcher(content);

            while (matcher.find()) {
                String groupId = matcher.group(1).trim();
                String artifactId = matcher.group(2).trim();
                String version = matcher.group(3).trim();

                String packageName = groupId + ":" + artifactId;
                
                Dependency dependency = new Dependency();
                dependency.setScan(scan);
                dependency.setPackageName(packageName);
                dependency.setCurrentVersion(version);
                
                // Check for known vulnerable versions (simplified)
                checkMavenVulnerability(dependency);
                
                dependencyJpaRepo.save(dependency);
                result.incrementTotal();
                if (dependency.getIsVulnerable()) {
                    result.incrementVulnerable();
                }
            }

        } catch (Exception e) {
            log.error("Failed to analyze Maven dependencies", e);
        }
    }

    private void analyzeNpmDependencies(String scanPath, Scan scan, DependencyAnalysisResult result) {
        try {
            Path packageJsonPath = Paths.get(scanPath, "package.json");
            if (!Files.exists(packageJsonPath)) {
                return;
            }

            log.info("Found package.json, analyzing NPM dependencies");
            String content = Files.readString(packageJsonPath);
            JsonObject root = gson.fromJson(content, JsonObject.class);

            if (root.has("dependencies")) {
                JsonObject deps = root.getAsJsonObject("dependencies");
                deps.entrySet().forEach(entry -> {
                    String packageName = entry.getKey();
                    String version = entry.getValue().getAsString().replace("^", "").replace("~", "");

                    Dependency dependency = new Dependency();
                    dependency.setScan(scan);
                    dependency.setPackageName(packageName);
                    dependency.setCurrentVersion(version);
                    
                    checkNpmVulnerability(dependency);
                    
                    dependencyJpaRepo.save(dependency);
                    result.incrementTotal();
                    if (dependency.getIsVulnerable()) {
                        result.incrementVulnerable();
                    }
                });
            }

        } catch (Exception e) {
            log.error("Failed to analyze NPM dependencies", e);
        }
    }

    private void analyzePythonDependencies(String scanPath, Scan scan, DependencyAnalysisResult result) {
        try {
            Path requirementsPath = Paths.get(scanPath, "requirements.txt");
            if (!Files.exists(requirementsPath)) {
                return;
            }

            log.info("Found requirements.txt, analyzing Python dependencies");
            List<String> lines = Files.readAllLines(requirementsPath);

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("==|>=|<=|>|<");
                if (parts.length == 0) continue;

                String packageName = parts[0].trim();
                String version = parts.length > 1 ? parts[1].trim() : "unknown";

                Dependency dependency = new Dependency();
                dependency.setScan(scan);
                dependency.setPackageName(packageName);
                dependency.setCurrentVersion(version);
                
                checkPythonVulnerability(dependency);
                
                dependencyJpaRepo.save(dependency);
                result.incrementTotal();
                if (dependency.getIsVulnerable()) {
                    result.incrementVulnerable();
                }
            }

        } catch (Exception e) {
            log.error("Failed to analyze Python dependencies", e);
        }
    }

    private void checkMavenVulnerability(Dependency dependency) {
        // Simplified vulnerability check - in production, use NVD API or Snyk API
        String packageName = dependency.getPackageName();
        String version = dependency.getCurrentVersion();

        // Known vulnerable versions (examples)
        if (packageName.contains("log4j-core") && version.startsWith("2.") && 
            compareVersion(version, "2.17.0") < 0) {
            dependency.setIsVulnerable(true);
            dependency.setVulnerabilityId("CVE-2021-44228");
            dependency.setSeverity(Issue.Severity.CRITICAL);
            dependency.setDescription("Log4Shell vulnerability");
            dependency.setFixVersion("2.17.0");
        } else if (packageName.contains("spring-core") && compareVersion(version, "5.3.18") < 0) {
            dependency.setIsVulnerable(true);
            dependency.setVulnerabilityId("CVE-2022-22965");
            dependency.setSeverity(Issue.Severity.CRITICAL);
            dependency.setDescription("Spring4Shell vulnerability");
            dependency.setFixVersion("5.3.18");
        } else {
            dependency.setIsVulnerable(false);
        }
    }

    private void checkNpmVulnerability(Dependency dependency) {
        String packageName = dependency.getPackageName();
        String version = dependency.getCurrentVersion();

        // Known vulnerable versions (examples)
        if (packageName.equals("lodash") && compareVersion(version, "4.17.21") < 0) {
            dependency.setIsVulnerable(true);
            dependency.setVulnerabilityId("CVE-2021-23337");
            dependency.setSeverity(Issue.Severity.HIGH);
            dependency.setDescription("Command injection vulnerability");
            dependency.setFixVersion("4.17.21");
        } else if (packageName.equals("axios") && compareVersion(version, "0.21.2") < 0) {
            dependency.setIsVulnerable(true);
            dependency.setVulnerabilityId("CVE-2021-3749");
            dependency.setSeverity(Issue.Severity.MEDIUM);
            dependency.setDescription("SSRF vulnerability");
            dependency.setFixVersion("0.21.2");
        } else {
            dependency.setIsVulnerable(false);
        }
    }

    private void checkPythonVulnerability(Dependency dependency) {
        String packageName = dependency.getPackageName();
        String version = dependency.getCurrentVersion();

        // Known vulnerable versions (examples)
        if (packageName.equals("django") && compareVersion(version, "3.2.13") < 0) {
            dependency.setIsVulnerable(true);
            dependency.setVulnerabilityId("CVE-2022-28346");
            dependency.setSeverity(Issue.Severity.HIGH);
            dependency.setDescription("SQL injection vulnerability");
            dependency.setFixVersion("3.2.13");
        } else if (packageName.equals("requests") && compareVersion(version, "2.20.0") < 0) {
            dependency.setIsVulnerable(true);
            dependency.setVulnerabilityId("CVE-2018-18074");
            dependency.setSeverity(Issue.Severity.MEDIUM);
            dependency.setDescription("Header injection vulnerability");
            dependency.setFixVersion("2.20.0");
        } else {
            dependency.setIsVulnerable(false);
        }
    }

    private int compareVersion(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    private int parseVersionPart(String part) {
        try {
            // Remove non-numeric suffixes (e.g., "RELEASE", "SNAPSHOT")
            part = part.replaceAll("[^0-9].*", "");
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @lombok.Data
    public static class DependencyAnalysisResult {
        private int totalDependencies;
        private int vulnerableDependencies;
        private String errorMessage;

        public void incrementTotal() {
            totalDependencies++;
        }

        public void incrementVulnerable() {
            vulnerableDependencies++;
        }
    }
}
