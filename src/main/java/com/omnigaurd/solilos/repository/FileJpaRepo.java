package com.omnigaurd.solilos.repository;

import com.omnigaurd.solilos.model.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FileJpaRepo extends JpaRepository<File, Long> {
    List<File> findByScanId(Long scanId);
    List<File> findByScanIdAndLanguage(Long scanId, String language);
}
