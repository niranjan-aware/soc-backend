package com.omnigaurd.solilos.controller;

import com.omnigaurd.solilos.model.dto.DashboardMetrics;
import com.omnigaurd.solilos.model.dto.RepositoryMetrics;
import com.omnigaurd.solilos.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public ResponseEntity<DashboardMetrics> getOverview() {
        return ResponseEntity.ok(dashboardService.getOverviewMetrics());
    }

    @GetMapping("/repository/{id}")
    public ResponseEntity<RepositoryMetrics> getRepositoryMetrics(@PathVariable Long id) {
        return ResponseEntity.ok(dashboardService.getRepositoryMetrics(id));
    }
}
