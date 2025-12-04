package com.omnigaurd.solilos.service.github;

import com.omnigaurd.solilos.model.entity.Repository;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GitHubService {

    @Value("${github.api.base-url}")
    private String githubApiUrl;

    public List<Repository> fetchUserRepositories(String username) throws IOException {
        log.info("Fetching repositories for user: {}", username);
        
        GitHub github = new GitHubBuilder().build();
        List<GHRepository> ghRepos = new ArrayList<>(github.getUser(username).listRepositories().toList());
        
        List<Repository> repositories = new ArrayList<>();
        
        for (GHRepository ghRepo : ghRepos) {
            if (!ghRepo.isPrivate()) { // Only public repos
                Repository repo = mapToRepository(ghRepo, username, null);
                repositories.add(repo);
            }
        }
        
        log.info("Found {} public repositories for user: {}", repositories.size(), username);
        return repositories;
    }

    public List<Repository> fetchOrgRepositories(String orgName) throws IOException {
        log.info("Fetching repositories for organization: {}", orgName);
        
        GitHub github = new GitHubBuilder().build();
        List<GHRepository> ghRepos = new ArrayList<>(github.getOrganization(orgName).listRepositories().toList());
        
        List<Repository> repositories = new ArrayList<>();
        
        for (GHRepository ghRepo : ghRepos) {
            if (!ghRepo.isPrivate()) { // Only public repos
                Repository repo = mapToRepository(ghRepo, null, orgName);
                repositories.add(repo);
            }
        }
        
        log.info("Found {} public repositories for organization: {}", repositories.size(), orgName);
        return repositories;
    }

    private Repository mapToRepository(GHRepository ghRepo, String username, String org) throws IOException {
        Repository repo = new Repository();
        repo.setGithubUsername(username);
        repo.setGithubOrg(org);
        repo.setRepoName(ghRepo.getName());
        repo.setRepoUrl(ghRepo.getHtmlUrl().toString());
        repo.setDefaultBranch(ghRepo.getDefaultBranch());
        repo.setLanguage(ghRepo.getLanguage());
        repo.setStars(ghRepo.getStargazersCount());
        repo.setLastUpdated(LocalDateTime.ofInstant(
            ghRepo.getUpdatedAt().toInstant(), 
            ZoneId.systemDefault()
        ));
        repo.setStatus(Repository.RepositoryStatus.ACTIVE);
        
        return repo;
    }
}
