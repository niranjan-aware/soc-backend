package com.omnigaurd.solilos.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {
    private Long scanId;
    private String analyzerSource;
    private int totalIssues;
    private int criticalCount;
    private int highCount;
    private int mediumCount;
    private int lowCount;
    private List<IssueDetail> issues = new ArrayList<>();
    private boolean success;
    private String errorMessage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueDetail {
        private String filePath;
        private String issueType;
        private String severity;
        private String ruleId;
        private String ruleName;
        private String description;
        private Integer lineNumber;
        private Integer columnNumber;
        private String codeSnippet;
        private String suggestedFix;
        private String cweId;
        private String owaspCategory;
        private String analyzerSource; // Added this field
    }
}
