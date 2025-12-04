package com.omnigaurd.solilos.model.dto;

import com.omnigaurd.solilos.model.entity.Scan;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScanResponse {
    private Long id;
    private Long repositoryId;
    private String repositoryName;
    private String scanType;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer totalFiles;
    private Integer totalIssues;
    private Integer criticalCount;
    private Integer highCount;
    private Integer mediumCount;
    private Integer lowCount;
    private Long scanDurationMs;
    private String errorMessage;

    public static ScanResponse from(Scan scan) {
        return new ScanResponse(
            scan.getId(),
            scan.getRepository().getId(),
            scan.getRepository().getRepoName(),
            scan.getScanType() != null ? scan.getScanType().name() : null,
            scan.getStatus() != null ? scan.getStatus().name() : null,
            scan.getStartedAt(),
            scan.getCompletedAt(),
            scan.getTotalFiles(),
            scan.getTotalIssues(),
            scan.getCriticalCount(),
            scan.getHighCount(),
            scan.getMediumCount(),
            scan.getLowCount(),
            scan.getScanDurationMs(),
            scan.getErrorMessage()
        );
    }
}
