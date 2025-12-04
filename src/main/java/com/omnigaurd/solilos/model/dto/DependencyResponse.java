package com.omnigaurd.solilos.model.dto;

import com.omnigaurd.solilos.model.entity.Dependency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DependencyResponse {
    private Long id;
    private String packageName;
    private String currentVersion;
    private String latestVersion;
    private Boolean isVulnerable;
    private String vulnerabilityId;
    private String severity;
    private String description;
    private String fixVersion;

    public static DependencyResponse from(Dependency dep) {
        return new DependencyResponse(
            dep.getId(),
            dep.getPackageName(),
            dep.getCurrentVersion(),
            dep.getLatestVersion(),
            dep.getIsVulnerable(),
            dep.getVulnerabilityId(),
            dep.getSeverity() != null ? dep.getSeverity().name() : null,
            dep.getDescription(),
            dep.getFixVersion()
        );
    }
}
