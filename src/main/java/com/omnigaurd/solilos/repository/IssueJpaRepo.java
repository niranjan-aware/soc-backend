package com.omnigaurd.solilos.repository;

import com.omnigaurd.solilos.model.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IssueJpaRepo extends JpaRepository<Issue, Long> {
    List<Issue> findByScanId(Long scanId);
    List<Issue> findByFileId(Long fileId);
    List<Issue> findByScanIdAndSeverity(Long scanId, Issue.Severity severity);
    List<Issue> findByScanIdAndIssueType(Long scanId, Issue.IssueType issueType);
    
    @Query("SELECT COUNT(i) FROM Issue i WHERE i.scan.id = :scanId AND (:severity IS NULL OR i.severity = :severity)")
    Long countByScanIdAndSeverity(Long scanId, Issue.Severity severity);
}
