package com.omnigaurd.solilos.controller;

import com.omnigaurd.solilos.model.dto.ImportResponse;
import com.omnigaurd.solilos.model.dto.RepositoryRequest;
import com.omnigaurd.solilos.model.dto.RepositoryResponse;
import com.omnigaurd.solilos.service.RepositoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repositories")
@RequiredArgsConstructor
public class RepositoryController {

    private final RepositoryService repositoryService;

    @PostMapping("/import")
    public ResponseEntity<ImportResponse> importRepositories(@RequestBody RepositoryRequest request) {
        ImportResponse response = repositoryService.importFromGitHub(
            request.getGithubUsername(),
            request.getGithubOrg(),
            request.getType()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<RepositoryResponse>> getAllRepositories() {
        return ResponseEntity.ok(repositoryService.getAllRepositories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepositoryResponse> getRepository(@PathVariable Long id) {
        return repositoryService.getRepositoryById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<List<RepositoryResponse>> getByUsername(@PathVariable String username) {
        return ResponseEntity.ok(repositoryService.getRepositoriesByUsername(username));
    }

    @GetMapping("/org/{org}")
    public ResponseEntity<List<RepositoryResponse>> getByOrg(@PathVariable String org) {
        return ResponseEntity.ok(repositoryService.getRepositoriesByOrg(org));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRepository(@PathVariable Long id) {
        repositoryService.deleteRepository(id);
        return ResponseEntity.noContent().build();
    }
}
