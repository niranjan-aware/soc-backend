package com.omnigaurd.solilos.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dependencies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dependency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id", nullable = false)
    private Scan scan;

    @Column(name = "package_name", nullable = false)
    private String packageName;

    @Column(name = "current_version")
    private String currentVersion;

    @Column(name = "latest_version")
    private String latestVersion;

    @Column(name = "is_vulnerable")
    private Boolean isVulnerable;

    @Column(name = "vulnerability_id")
    private String vulnerabilityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private Issue.Severity severity;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "fix_version")
    private String fixVersion;
}
