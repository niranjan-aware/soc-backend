package com.omnigaurd.solilos.repository;

import com.omnigaurd.solilos.model.entity.ScanMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScanMetricsJpaRepo extends JpaRepository<ScanMetrics, Long> {
    List<ScanMetrics> findByScanId(Long scanId);
}
