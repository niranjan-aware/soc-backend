package com.omnigaurd.solilos.controller;

import com.omnigaurd.solilos.model.dto.DependencyResponse;
import com.omnigaurd.solilos.service.DependencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dependencies")
@RequiredArgsConstructor
public class DependencyController {

    private final DependencyService dependencyService;

    @GetMapping("/scan/{scanId}")
    public ResponseEntity<List<DependencyResponse>> getDependenciesByScan(@PathVariable Long scanId) {
        return ResponseEntity.ok(dependencyService.getDependenciesByScan(scanId));
    }

    @GetMapping("/scan/{scanId}/vulnerable")
    public ResponseEntity<List<DependencyResponse>> getVulnerableDependencies(@PathVariable Long scanId) {
        return ResponseEntity.ok(dependencyService.getVulnerableDependencies(scanId));
    }
}
