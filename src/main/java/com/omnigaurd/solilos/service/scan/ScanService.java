package com.omnigaurd.solilos.service.scan;

import com.omnigaurd.solilos.model.dto.ScanResponse;
import com.omnigaurd.solilos.model.entity.Repository;
import com.omnigaurd.solilos.model.entity.Scan;
import com.omnigaurd.solilos.repository.RepositoryJpaRepo;
import com.omnigaurd.solilos.repository.ScanJpaRepo;
import com.omnigaurd.solilos.service.github.GitCloneService;
import com.omnigaurd.solilos.service.storage.FileStorageService;
import com.omnigaurd.solilos.worker.ScanWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScanService {

    private final ScanJpaRepo scanJpaRepo;
    private final RepositoryJpaRepo repositoryJpaRepo;
    private final GitCloneService gitCloneService;
    private final FileStorageService fileStorageService;
    private final ScanQueueService scanQueueService;
    private final ScanWorker scanWorker;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ScanResponse initiateScan(Long repositoryId, String branch) {
        log.info("Initiating scan for repository: {}", repositoryId);

        Repository repository = repositoryJpaRepo.findById(repositoryId)
            .orElseThrow(() -> new RuntimeException("Repository not found: " + repositoryId));

        Scan scan = new Scan();
        scan.setRepository(repository);
        scan.setScanType(Scan.ScanType.FULL);
        scan.setStatus(Scan.ScanStatus.QUEUED);
        scan.setStartedAt(LocalDateTime.now());
        
        scan = scanJpaRepo.save(scan);
        scanJpaRepo.flush(); // Force immediate save
        
        Long scanId = scan.getId();
        log.info("Created scan record with ID: {}", scanId);

        String branchToUse = (branch != null && !branch.isEmpty()) ? branch : repository.getDefaultBranch();
        GitCloneService.CloneResult cloneResult = gitCloneService.cloneRepository(
            repository.getRepoUrl(),
            scanId,
            branchToUse
        );

        if (!cloneResult.isSuccess()) {
            scan.setStatus(Scan.ScanStatus.FAILED);
            scan.setErrorMessage("Failed to clone repository: " + cloneResult.getErrorMessage());
            scan.setCompletedAt(LocalDateTime.now());
            scanJpaRepo.save(scan);
            
            log.error("Scan failed for repository {}: {}", repositoryId, cloneResult.getErrorMessage());
            return ScanResponse.from(scan);
        }

        scan.setTotalFiles(cloneResult.getTotalFiles());
        scan = scanJpaRepo.save(scan);
        scanJpaRepo.flush(); // Ensure save is committed

        log.info("Repository cloned successfully. Starting async analysis...");

        // Enqueue and trigger async processing
        scanQueueService.enqueueScan(scanId);
        
        // Use the scanId directly instead of scan object
        final Long finalScanId = scanId;
        new Thread(() -> scanWorker.processScan(finalScanId)).start();

        return ScanResponse.from(scan);
    }

    public List<ScanResponse> getAllScans() {
        return scanJpaRepo.findAll().stream()
            .map(ScanResponse::from)
            .collect(Collectors.toList());
    }

    public Optional<ScanResponse> getScanById(Long id) {
        return scanJpaRepo.findById(id)
            .map(ScanResponse::from);
    }

    public List<ScanResponse> getScansByRepository(Long repositoryId) {
        return scanJpaRepo.findByRepositoryIdOrderByStartedAtDesc(repositoryId).stream()
            .map(ScanResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional
    public void deleteScan(Long scanId) {
        fileStorageService.deleteScanDirectory(scanId);
        scanJpaRepo.deleteById(scanId);
        log.info("Deleted scan: {}", scanId);
    }

    public ScanQueueService.ScanProgress getScanProgress(Long scanId) {
        return scanQueueService.getScanProgress(scanId);
    }
}
