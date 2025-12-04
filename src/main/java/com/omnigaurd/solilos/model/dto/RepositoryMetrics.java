package com.omnigaurd.solilos.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryMetrics {
    private Long repositoryId;
    private String repositoryName;
    private int totalScans;
    private int totalIssues;
    private int criticalIssues;
    private int highIssues;
    private int mediumIssues;
    private int lowIssues;
    private int totalVulnerableDependencies;
    private String lastScanStatus;
    private String lastScanDate;
    private List<ScanSummary> recentScans;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScanSummary {
        private Long scanId;
        private String status;
        private String startedAt;
        private int totalIssues;
        private int criticalCount;
        private int highCount;
    }
}
