package com.omnigaurd.solilos.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "scans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Scan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_type")
    private ScanType scanType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ScanStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "total_files")
    private Integer totalFiles;

    @Column(name = "total_issues")
    private Integer totalIssues;

    @Column(name = "critical_count")
    private Integer criticalCount;

    @Column(name = "high_count")
    private Integer highCount;

    @Column(name = "medium_count")
    private Integer mediumCount;

    @Column(name = "low_count")
    private Integer lowCount;

    @Column(name = "scan_duration_ms")
    private Long scanDurationMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = ScanStatus.QUEUED;
        }
        if (scanType == null) {
            scanType = ScanType.FULL;
        }
        startedAt = LocalDateTime.now();
        totalFiles = 0;
        totalIssues = 0;
        criticalCount = 0;
        highCount = 0;
        mediumCount = 0;
        lowCount = 0;
    }

    public enum ScanType {
        FULL, INCREMENTAL
    }

    public enum ScanStatus {
        QUEUED, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
    }
}
