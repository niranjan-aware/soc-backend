package com.omnigaurd.solilos.repository;

import com.omnigaurd.solilos.model.entity.Dependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DependencyJpaRepo extends JpaRepository<Dependency, Long> {
    List<Dependency> findByScanId(Long scanId);
    List<Dependency> findByScanIdAndIsVulnerable(Long scanId, Boolean isVulnerable);
}
