package com.omnigaurd.solilos.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetrics {
    private int totalRepositories;
    private int totalScans;
    private int completedScans;
    private int failedScans;
    private int inProgressScans;
    private int totalIssues;
    private int criticalIssues;
    private int highIssues;
    private int mediumIssues;
    private int lowIssues;
    private int totalVulnerableDependencies;
    private Map<String, Integer> issuesByType;
    private Map<String, Integer> issuesBySeverity;
    private Map<String, Integer> issuesByAnalyzer;
    private RecentActivity recentActivity;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private int scansLast24Hours;
        private int scansLast7Days;
        private int scansLast30Days;
        private int issuesFoundLast24Hours;
        private int issuesFoundLast7Days;
    }
}
