package com.omnigaurd.solilos.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "issues")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Issue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id", nullable = false)
    private Scan scan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type")
    private IssueType issueType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private Severity severity;

    @Column(name = "rule_id")
    private String ruleId;

    @Column(name = "rule_name", length = 500)
    private String ruleName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "column_number")
    private Integer columnNumber;

    @Column(name = "code_snippet", columnDefinition = "TEXT")
    private String codeSnippet;

    @Column(name = "suggested_fix", columnDefinition = "TEXT")
    private String suggestedFix;

    @Column(name = "cwe_id")
    private String cweId;

    @Column(name = "owasp_category")
    private String owaspCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "analyzer_source")
    private AnalyzerSource analyzerSource;

    @Column(name = "confidence", precision = 3, scale = 2)
    private BigDecimal confidence;

    @Column(name = "false_positive")
    private Boolean falsePositive = false;

    public enum IssueType {
        SECURITY, VULNERABILITY, CODE_QUALITY, MISCONFIGURATION
    }

    public enum Severity {
        CRITICAL, HIGH, MEDIUM, LOW, INFO
    }

    public enum AnalyzerSource {
        SEMGREP, TREE_SITTER, AI, DEPENDENCY_CHECK
    }
}
