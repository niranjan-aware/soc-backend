package com.omnigaurd.solilos.controller;

import com.omnigaurd.solilos.model.dto.ScanRequest;
import com.omnigaurd.solilos.model.dto.ScanResponse;
import com.omnigaurd.solilos.service.scan.ScanQueueService;
import com.omnigaurd.solilos.service.scan.ScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/scans")
@RequiredArgsConstructor
public class ScanController {

    private final ScanService scanService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @PostMapping
    public ResponseEntity<ScanResponse> createScan(@RequestBody ScanRequest request) {
        ScanResponse response = scanService.initiateScan(
            request.getRepositoryId(),
            request.getBranch()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ScanResponse>> getAllScans() {
        return ResponseEntity.ok(scanService.getAllScans());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScanResponse> getScan(@PathVariable Long id) {
        return scanService.getScanById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/repository/{repositoryId}")
    public ResponseEntity<List<ScanResponse>> getScansByRepository(@PathVariable Long repositoryId) {
        return ResponseEntity.ok(scanService.getScansByRepository(repositoryId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScan(@PathVariable Long id) {
        scanService.deleteScan(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/progress")
    public ResponseEntity<ScanQueueService.ScanProgress> getScanProgress(@PathVariable Long id) {
        return ResponseEntity.ok(scanService.getScanProgress(id));
    }

    @GetMapping(value = "/{id}/progress/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamScanProgress(@PathVariable Long id) {
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes timeout

        executor.execute(() -> {
            try {
                while (true) {
                    ScanQueueService.ScanProgress progress = scanService.getScanProgress(id);
                    emitter.send(progress);

                    if (progress.progress() >= 100) {
                        emitter.complete();
                        break;
                    }

                    Thread.sleep(2000); // Update every 2 seconds
                }
            } catch (IOException | InterruptedException e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
