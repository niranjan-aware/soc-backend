package com.omnigaurd.solilos.repository;

import com.omnigaurd.solilos.model.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

@org.springframework.stereotype.Repository
public interface RepositoryJpaRepo extends JpaRepository<Repository, Long> {
    Optional<Repository> findByRepoUrl(String repoUrl);
    List<Repository> findByGithubUsername(String githubUsername);
    List<Repository> findByGithubOrg(String githubOrg);
    List<Repository> findByStatus(Repository.RepositoryStatus status);
}
