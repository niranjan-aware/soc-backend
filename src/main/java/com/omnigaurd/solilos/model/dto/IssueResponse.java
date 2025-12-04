package com.omnigaurd.solilos.model.dto;

import com.omnigaurd.solilos.model.entity.Issue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueResponse {
    private Long id;
    private Long scanId;
    private Long fileId;
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
    private String analyzerSource;
    private Boolean falsePositive;

    public static IssueResponse from(Issue issue) {
        return new IssueResponse(
            issue.getId(),
            issue.getScan().getId(),
            issue.getFile().getId(),
            issue.getFile().getFilePath(),
            issue.getIssueType() != null ? issue.getIssueType().name() : null,
            issue.getSeverity() != null ? issue.getSeverity().name() : null,
            issue.getRuleId(),
            issue.getRuleName(),
            issue.getDescription(),
            issue.getLineNumber(),
            issue.getColumnNumber(),
            issue.getCodeSnippet(),
            issue.getSuggestedFix(),
            issue.getCweId(),
            issue.getOwaspCategory(),
            issue.getAnalyzerSource() != null ? issue.getAnalyzerSource().name() : null,
            issue.getFalsePositive()
        );
    }
}
