package com.omnigaurd.solilos.service;

import com.omnigaurd.solilos.model.dto.IssueResponse;
import com.omnigaurd.solilos.model.entity.Issue;
import com.omnigaurd.solilos.repository.IssueJpaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueService {

    private final IssueJpaRepo issueJpaRepo;

    public List<IssueResponse> getIssuesByScan(Long scanId) {
        return issueJpaRepo.findByScanId(scanId).stream()
            .map(IssueResponse::from)
            .collect(Collectors.toList());
    }

    public Optional<IssueResponse> getIssueById(Long id) {
        return issueJpaRepo.findById(id)
            .map(IssueResponse::from);
    }

    public List<IssueResponse> getIssuesBySeverity(Long scanId, String severity) {
        Issue.Severity sev = Issue.Severity.valueOf(severity.toUpperCase());
        return issueJpaRepo.findByScanIdAndSeverity(scanId, sev).stream()
            .map(IssueResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional
    public Optional<IssueResponse> markAsFalsePositive(Long id) {
        return issueJpaRepo.findById(id)
            .map(issue -> {
                issue.setFalsePositive(true);
                issueJpaRepo.save(issue);
                return IssueResponse.from(issue);
            });
    }
}
