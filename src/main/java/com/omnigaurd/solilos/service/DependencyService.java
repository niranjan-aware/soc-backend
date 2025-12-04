package com.omnigaurd.solilos.service;

import com.omnigaurd.solilos.model.dto.DependencyResponse;
import com.omnigaurd.solilos.repository.DependencyJpaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DependencyService {

    private final DependencyJpaRepo dependencyJpaRepo;

    public List<DependencyResponse> getDependenciesByScan(Long scanId) {
        return dependencyJpaRepo.findByScanId(scanId).stream()
            .map(DependencyResponse::from)
            .collect(Collectors.toList());
    }

    public List<DependencyResponse> getVulnerableDependencies(Long scanId) {
        return dependencyJpaRepo.findByScanIdAndIsVulnerable(scanId, true).stream()
            .map(DependencyResponse::from)
            .collect(Collectors.toList());
    }
}
