package com.omnigaurd.solilos.service.dashboard;

import com.omnigaurd.solilos.model.dto.DashboardMetrics;
import com.omnigaurd.solilos.model.dto.RepositoryMetrics;
import com.omnigaurd.solilos.model.entity.Issue;
import com.omnigaurd.solilos.model.entity.Scan;
import com.omnigaurd.solilos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final RepositoryJpaRepo repositoryJpaRepo;
    private final ScanJpaRepo scanJpaRepo;
    private final IssueJpaRepo issueJpaRepo;
    private final DependencyJpaRepo dependencyJpaRepo;

    public DashboardMetrics getOverviewMetrics() {
        DashboardMetrics metrics = new DashboardMetrics();

        // Repository metrics
        metrics.setTotalRepositories((int) repositoryJpaRepo.count());

        // Scan metrics
        List<Scan> allScans = scanJpaRepo.findAll();
        metrics.setTotalScans(allScans.size());
        metrics.setCompletedScans((int) allScans.stream()
            .filter(s -> s.getStatus() == Scan.ScanStatus.COMPLETED).count());
        metrics.setFailedScans((int) allScans.stream()
            .filter(s -> s.getStatus() == Scan.ScanStatus.FAILED).count());
        metrics.setInProgressScans((int) allScans.stream()
            .filter(s -> s.getStatus() == Scan.ScanStatus.IN_PROGRESS || 
                        s.getStatus() == Scan.ScanStatus.QUEUED).count());

        // Issue metrics
        List<Issue> allIssues = issueJpaRepo.findAll();
        metrics.setTotalIssues(allIssues.size());
        metrics.setCriticalIssues((int) allIssues.stream()
            .filter(i -> i.getSeverity() == Issue.Severity.CRITICAL).count());
        metrics.setHighIssues((int) allIssues.stream()
            .filter(i -> i.getSeverity() == Issue.Severity.HIGH).count());
        metrics.setMediumIssues((int) allIssues.stream()
            .filter(i -> i.getSeverity() == Issue.Severity.MEDIUM).count());
        metrics.setLowIssues((int) allIssues.stream()
            .filter(i -> i.getSeverity() == Issue.Severity.LOW).count());

        // Vulnerable dependencies
        metrics.setTotalVulnerableDependencies(
            (int) dependencyJpaRepo.findAll().stream()
                .filter(d -> d.getIsVulnerable() != null && d.getIsVulnerable()).count()
        );

        // Issues by type
        Map<String, Integer> issuesByType = new HashMap<>();
        for (Issue issue : allIssues) {
            String type = issue.getIssueType() != null ? issue.getIssueType().name() : "UNKNOWN";
            issuesByType.merge(type, 1, Integer::sum);
        }
        metrics.setIssuesByType(issuesByType);

        // Issues by severity
        Map<String, Integer> issuesBySeverity = new HashMap<>();
        issuesBySeverity.put("CRITICAL", metrics.getCriticalIssues());
        issuesBySeverity.put("HIGH", metrics.getHighIssues());
        issuesBySeverity.put("MEDIUM", metrics.getMediumIssues());
        issuesBySeverity.put("LOW", metrics.getLowIssues());
        metrics.setIssuesBySeverity(issuesBySeverity);

        // Issues by analyzer
        Map<String, Integer> issuesByAnalyzer = new HashMap<>();
        for (Issue issue : allIssues) {
            String analyzer = issue.getAnalyzerSource() != null ? 
                issue.getAnalyzerSource().name() : "UNKNOWN";
            issuesByAnalyzer.merge(analyzer, 1, Integer::sum);
        }
        metrics.setIssuesByAnalyzer(issuesByAnalyzer);

        // Recent activity
        metrics.setRecentActivity(calculateRecentActivity(allScans, allIssues));

        return metrics;
    }

    public RepositoryMetrics getRepositoryMetrics(Long repositoryId) {
        var repository = repositoryJpaRepo.findById(repositoryId)
            .orElseThrow(() -> new RuntimeException("Repository not found"));

        RepositoryMetrics metrics = new RepositoryMetrics();
        metrics.setRepositoryId(repositoryId);
        metrics.setRepositoryName(repository.getRepoName());

        List<Scan> scans = scanJpaRepo.findByRepositoryIdOrderByStartedAtDesc(repositoryId);
        metrics.setTotalScans(scans.size());

        // Aggregate issue counts from all scans
        int totalIssues = scans.stream().mapToInt(s -> s.getTotalIssues() != null ? s.getTotalIssues() : 0).sum();
        int critical = scans.stream().mapToInt(s -> s.getCriticalCount() != null ? s.getCriticalCount() : 0).sum();
        int high = scans.stream().mapToInt(s -> s.getHighCount() != null ? s.getHighCount() : 0).sum();
        int medium = scans.stream().mapToInt(s -> s.getMediumCount() != null ? s.getMediumCount() : 0).sum();
        int low = scans.stream().mapToInt(s -> s.getLowCount() != null ? s.getLowCount() : 0).sum();

        metrics.setTotalIssues(totalIssues);
        metrics.setCriticalIssues(critical);
        metrics.setHighIssues(high);
        metrics.setMediumIssues(medium);
        metrics.setLowIssues(low);

        // Vulnerable dependencies
        int vulnDeps = scans.stream()
            .mapToInt(scan -> (int) dependencyJpaRepo.findByScanIdAndIsVulnerable(scan.getId(), true).size())
            .sum();
        metrics.setTotalVulnerableDependencies(vulnDeps);

        // Last scan info
        if (!scans.isEmpty()) {
            Scan lastScan = scans.get(0);
            metrics.setLastScanStatus(lastScan.getStatus().name());
            metrics.setLastScanDate(lastScan.getStartedAt().toString());
        }

        // Recent scans summary
        List<RepositoryMetrics.ScanSummary> recentScans = scans.stream()
            .limit(5)
            .map(scan -> new RepositoryMetrics.ScanSummary(
                scan.getId(),
                scan.getStatus().name(),
                scan.getStartedAt().toString(),
                scan.getTotalIssues() != null ? scan.getTotalIssues() : 0,
                scan.getCriticalCount() != null ? scan.getCriticalCount() : 0,
                scan.getHighCount() != null ? scan.getHighCount() : 0
            ))
            .collect(Collectors.toList());
        metrics.setRecentScans(recentScans);

        return metrics;
    }

    private DashboardMetrics.RecentActivity calculateRecentActivity(List<Scan> scans, List<Issue> issues) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last24h = now.minusHours(24);
        LocalDateTime last7d = now.minusDays(7);
        LocalDateTime last30d = now.minusDays(30);

        int scansLast24h = (int) scans.stream()
            .filter(s -> s.getStartedAt().isAfter(last24h)).count();
        int scansLast7d = (int) scans.stream()
            .filter(s -> s.getStartedAt().isAfter(last7d)).count();
        int scansLast30d = (int) scans.stream()
            .filter(s -> s.getStartedAt().isAfter(last30d)).count();

        // For issues, we need to check scan dates
        int issuesLast24h = (int) issues.stream()
            .filter(i -> i.getScan().getStartedAt().isAfter(last24h)).count();
        int issuesLast7d = (int) issues.stream()
            .filter(i -> i.getScan().getStartedAt().isAfter(last7d)).count();

        return new DashboardMetrics.RecentActivity(
            scansLast24h, scansLast7d, scansLast30d, issuesLast24h, issuesLast7d
        );
    }
}
