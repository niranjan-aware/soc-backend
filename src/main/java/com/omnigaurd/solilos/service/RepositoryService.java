package com.omnigaurd.solilos.service;

import com.omnigaurd.solilos.model.dto.ImportResponse;
import com.omnigaurd.solilos.model.dto.RepositoryResponse;
import com.omnigaurd.solilos.model.entity.Repository;
import com.omnigaurd.solilos.repository.RepositoryJpaRepo;
import com.omnigaurd.solilos.service.github.GitHubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepositoryService {

    private final RepositoryJpaRepo repositoryJpaRepo;
    private final GitHubService gitHubService;

    @Transactional
    public ImportResponse importFromGitHub(String username, String org, String type) {
        List<Repository> fetchedRepos = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        try {
            if ("username".equalsIgnoreCase(type)) {
                fetchedRepos = gitHubService.fetchUserRepositories(username);
            } else if ("org".equalsIgnoreCase(type)) {
                fetchedRepos = gitHubService.fetchOrgRepositories(org);
            } else {
                errors.add("Invalid type. Must be 'username' or 'org'");
                return new ImportResponse(0, 0, 0, errors, List.of());
            }
        } catch (Exception e) {
            log.error("Error fetching repositories from GitHub", e);
            errors.add("Failed to fetch from GitHub: " + e.getMessage());
            return new ImportResponse(0, 0, 0, errors, List.of());
        }

        int imported = 0;
        int alreadyExists = 0;
        List<Repository> savedRepos = new ArrayList<>();

        for (Repository repo : fetchedRepos) {
            Optional<Repository> existing = repositoryJpaRepo.findByRepoUrl(repo.getRepoUrl());
            
            if (existing.isPresent()) {
                alreadyExists++;
                savedRepos.add(existing.get());
            } else {
                Repository saved = repositoryJpaRepo.save(repo);
                imported++;
                savedRepos.add(saved);
            }
        }

        List<RepositoryResponse> responses = savedRepos.stream()
            .map(RepositoryResponse::from)
            .collect(Collectors.toList());

        return new ImportResponse(
            fetchedRepos.size(),
            imported,
            alreadyExists,
            errors,
            responses
        );
    }

    public List<RepositoryResponse> getAllRepositories() {
        return repositoryJpaRepo.findAll().stream()
            .map(RepositoryResponse::from)
            .collect(Collectors.toList());
    }

    public Optional<RepositoryResponse> getRepositoryById(Long id) {
        return repositoryJpaRepo.findById(id)
            .map(RepositoryResponse::from);
    }

    public List<RepositoryResponse> getRepositoriesByUsername(String username) {
        return repositoryJpaRepo.findByGithubUsername(username).stream()
            .map(RepositoryResponse::from)
            .collect(Collectors.toList());
    }

    public List<RepositoryResponse> getRepositoriesByOrg(String org) {
        return repositoryJpaRepo.findByGithubOrg(org).stream()
            .map(RepositoryResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional
    public void deleteRepository(Long id) {
        repositoryJpaRepo.deleteById(id);
    }
}
