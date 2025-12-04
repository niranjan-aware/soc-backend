package com.omnigaurd.solilos.service;

import com.omnigaurd.solilos.model.dto.FileResponse;
import com.omnigaurd.solilos.model.entity.File;
import com.omnigaurd.solilos.repository.FileJpaRepo;
import com.omnigaurd.solilos.repository.IssueJpaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileJpaRepo fileJpaRepo;
    private final IssueJpaRepo issueJpaRepo;

    public List<FileResponse> getFilesByScan(Long scanId) {
        return fileJpaRepo.findByScanId(scanId).stream()
            .map(file -> {
                int issueCount = issueJpaRepo.findByFileId(file.getId()).size();
                return FileResponse.from(file, issueCount);
            })
            .collect(Collectors.toList());
    }

    public List<FileResponse> getFilesWithIssues(Long scanId) {
        return fileJpaRepo.findByScanId(scanId).stream()
            .map(file -> {
                int issueCount = issueJpaRepo.findByFileId(file.getId()).size();
                return FileResponse.from(file, issueCount);
            })
            .filter(f -> f.getIssueCount() > 0)
            .collect(Collectors.toList());
    }
}
