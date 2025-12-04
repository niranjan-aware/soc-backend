package com.omnigaurd.solilos.model.dto;

import com.omnigaurd.solilos.model.entity.Repository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryResponse {
    private Long id;
    private String githubUsername;
    private String githubOrg;
    private String repoName;
    private String repoUrl;
    private String defaultBranch;
    private String language;
    private Integer stars;
    private LocalDateTime lastUpdated;
    private LocalDateTime createdAt;
    private String status;

    public static RepositoryResponse from(Repository repo) {
        return new RepositoryResponse(
            repo.getId(),
            repo.getGithubUsername(),
            repo.getGithubOrg(),
            repo.getRepoName(),
            repo.getRepoUrl(),
            repo.getDefaultBranch(),
            repo.getLanguage(),
            repo.getStars(),
            repo.getLastUpdated(),
            repo.getCreatedAt(),
            repo.getStatus() != null ? repo.getStatus().name() : null
        );
    }
}
