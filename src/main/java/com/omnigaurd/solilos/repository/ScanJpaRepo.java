package com.omnigaurd.solilos.repository;

import com.omnigaurd.solilos.model.entity.Scan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScanJpaRepo extends JpaRepository<Scan, Long> {
    List<Scan> findByRepositoryId(Long repositoryId);
    List<Scan> findByStatus(Scan.ScanStatus status);
    List<Scan> findByRepositoryIdOrderByStartedAtDesc(Long repositoryId);
}
