package com.omnigaurd.solilos.controller;

import com.omnigaurd.solilos.service.report.PdfReportService;
import com.omnigaurd.solilos.service.report.ReportGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportGeneratorService reportGeneratorService;
    private final PdfReportService pdfReportService;

    @GetMapping("/scan/{scanId}/pdf")
    public ResponseEntity<Resource> downloadPdfReport(@PathVariable Long scanId) {
        try {
            File report = pdfReportService.generatePdfReport(scanId);
            Resource resource = new FileSystemResource(report);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=scan-" + scanId + "-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/scan/{scanId}/json")
    public ResponseEntity<Resource> downloadJsonReport(@PathVariable Long scanId) {
        try {
            File report = reportGeneratorService.generateJsonReport(scanId);
            Resource resource = new FileSystemResource(report);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=scan-" + scanId + "-report.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/scan/{scanId}/csv")
    public ResponseEntity<Resource> downloadCsvReport(@PathVariable Long scanId) {
        try {
            File report = reportGeneratorService.generateCsvReport(scanId);
            Resource resource = new FileSystemResource(report);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=scan-" + scanId + "-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/scan/{scanId}/text")
    public ResponseEntity<Resource> downloadTextReport(@PathVariable Long scanId) {
        try {
            File report = reportGeneratorService.generateTextReport(scanId);
            Resource resource = new FileSystemResource(report);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=scan-" + scanId + "-report.txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
