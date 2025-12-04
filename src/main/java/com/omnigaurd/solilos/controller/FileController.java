package com.omnigaurd.solilos.controller;

import com.omnigaurd.solilos.model.dto.FileResponse;
import com.omnigaurd.solilos.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @GetMapping("/scan/{scanId}")
    public ResponseEntity<List<FileResponse>> getFilesByScan(@PathVariable Long scanId) {
        return ResponseEntity.ok(fileService.getFilesByScan(scanId));
    }

    @GetMapping("/scan/{scanId}/with-issues")
    public ResponseEntity<List<FileResponse>> getFilesWithIssues(@PathVariable Long scanId) {
        return ResponseEntity.ok(fileService.getFilesWithIssues(scanId));
    }
}
