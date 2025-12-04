package com.omnigaurd.solilos.controller;

import com.omnigaurd.solilos.model.dto.IssueResponse;
import com.omnigaurd.solilos.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    @GetMapping("/scan/{scanId}")
    public ResponseEntity<List<IssueResponse>> getIssuesByScan(@PathVariable Long scanId) {
        return ResponseEntity.ok(issueService.getIssuesByScan(scanId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IssueResponse> getIssue(@PathVariable Long id) {
        return issueService.getIssueById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/scan/{scanId}/severity/{severity}")
    public ResponseEntity<List<IssueResponse>> getIssuesBySeverity(
            @PathVariable Long scanId,
            @PathVariable String severity) {
        return ResponseEntity.ok(issueService.getIssuesBySeverity(scanId, severity));
    }

    @PutMapping("/{id}/mark-false-positive")
    public ResponseEntity<IssueResponse> markAsFalsePositive(@PathVariable Long id) {
        return issueService.markAsFalsePositive(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
