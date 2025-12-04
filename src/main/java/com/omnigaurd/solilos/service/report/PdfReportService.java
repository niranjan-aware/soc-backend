package com.omnigaurd.solilos.service.report;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.omnigaurd.solilos.model.entity.Dependency;
import com.omnigaurd.solilos.model.entity.Issue;
import com.omnigaurd.solilos.model.entity.Scan;
import com.omnigaurd.solilos.repository.DependencyJpaRepo;
import com.omnigaurd.solilos.repository.IssueJpaRepo;
import com.omnigaurd.solilos.repository.ScanJpaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfReportService {

    private final ScanJpaRepo scanJpaRepo;
    private final IssueJpaRepo issueJpaRepo;
    private final DependencyJpaRepo dependencyJpaRepo;

    public File generatePdfReport(Long scanId) throws Exception {
        Scan scan = scanJpaRepo.findById(scanId)
            .orElseThrow(() -> new RuntimeException("Scan not found"));

        List<Issue> issues = issueJpaRepo.findByScanId(scanId);
        List<Dependency> vulnDeps = dependencyJpaRepo.findByScanIdAndIsVulnerable(scanId, true);

        File reportFile = File.createTempFile("scan-report-" + scanId + "-", ".pdf");
        
        PdfWriter writer = new PdfWriter(new FileOutputStream(reportFile));
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Title
        Paragraph title = new Paragraph("SECURITY SCAN REPORT")
            .setFontSize(24)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20);
        document.add(title);

        // Scan Info
        document.add(new Paragraph("Repository: " + scan.getRepository().getRepoName()).setBold());
        document.add(new Paragraph("Scan ID: " + scanId));
        document.add(new Paragraph("Status: " + scan.getStatus().name()));
        document.add(new Paragraph("Started: " + scan.getStartedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        if (scan.getCompletedAt() != null) {
            document.add(new Paragraph("Completed: " + scan.getCompletedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
        document.add(new Paragraph("\n"));

        // Summary Table
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
        summaryTable.setWidth(UnitValue.createPercentValue(100));
        
        addSummaryRow(summaryTable, "Total Files Scanned", String.valueOf(scan.getTotalFiles()));
        addSummaryRow(summaryTable, "Total Issues Found", String.valueOf(scan.getTotalIssues()));

        // FIXED: DeviceRgb instead of ColorConstants
        addSummaryRow(summaryTable, "Critical Issues",
                String.valueOf(scan.getCriticalCount()),
                new DeviceRgb(255, 0, 0));  // RED

        addSummaryRow(summaryTable, "High Issues",
                String.valueOf(scan.getHighCount()),
                new DeviceRgb(255, 140, 0));

        addSummaryRow(summaryTable, "Medium Issues",
                String.valueOf(scan.getMediumCount()),
                new DeviceRgb(255, 215, 0));

        // FIXED: DeviceRgb instead of ColorConstants
        addSummaryRow(summaryTable, "Low Issues",
                String.valueOf(scan.getLowCount()),
                new DeviceRgb(0, 128, 0));  // GREEN

        addSummaryRow(summaryTable, "Vulnerable Dependencies", String.valueOf(vulnDeps.size()));
        
        document.add(summaryTable);
        document.add(new Paragraph("\n"));

        // Issues Table
        if (!issues.isEmpty()) {
            document.add(new Paragraph("DETAILED ISSUES").setBold().setFontSize(16));
            
            Table issuesTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1, 3}));
            issuesTable.setWidth(UnitValue.createPercentValue(100));
            
            // Header
            issuesTable.addHeaderCell(new Cell().add(new Paragraph("Severity").setBold()));
            issuesTable.addHeaderCell(new Cell().add(new Paragraph("File").setBold()));
            issuesTable.addHeaderCell(new Cell().add(new Paragraph("Line").setBold()));
            issuesTable.addHeaderCell(new Cell().add(new Paragraph("Description").setBold()));
            
            // Data
            for (Issue issue : issues.subList(0, Math.min(50, issues.size()))) {
                issuesTable.addCell(issue.getSeverity().name());
                issuesTable.addCell(issue.getFile().getFilePath());
                issuesTable.addCell(String.valueOf(issue.getLineNumber() != null ? issue.getLineNumber() : "-"));
                issuesTable.addCell(issue.getDescription());
            }
            
            document.add(issuesTable);
            
            if (issues.size() > 50) {
                document.add(new Paragraph("\n... and " + (issues.size() - 50) + " more issues").setItalic());
            }
        }

        // Vulnerable Dependencies
        if (!vulnDeps.isEmpty()) {
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("VULNERABLE DEPENDENCIES").setBold().setFontSize(16));
            
            Table depsTable = new Table(UnitValue.createPercentArray(new float[]{2, 1, 1, 3}));
            depsTable.setWidth(UnitValue.createPercentValue(100));
            
            depsTable.addHeaderCell(new Cell().add(new Paragraph("Package").setBold()));
            depsTable.addHeaderCell(new Cell().add(new Paragraph("Version").setBold()));
            depsTable.addHeaderCell(new Cell().add(new Paragraph("CVE").setBold()));
            depsTable.addHeaderCell(new Cell().add(new Paragraph("Description").setBold()));
            
            for (Dependency dep : vulnDeps) {
                depsTable.addCell(dep.getPackageName());
                depsTable.addCell(dep.getCurrentVersion());
                depsTable.addCell(dep.getVulnerabilityId() != null ? dep.getVulnerabilityId() : "-");
                depsTable.addCell(dep.getDescription() != null ? dep.getDescription() : "-");
            }
            
            document.add(depsTable);
        }

        // Footer
        document.add(new Paragraph("\n\n"));
        document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .setFontSize(10)
            .setTextAlignment(TextAlignment.CENTER)
            .setItalic());
        document.add(new Paragraph("Solilos Security Scanner")
            .setFontSize(10)
            .setTextAlignment(TextAlignment.CENTER)
            .setItalic());

        document.close();
        log.info("Generated PDF report: {}", reportFile.getAbsolutePath());
        
        return reportFile;
    }

    private void addSummaryRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label)));
        table.addCell(new Cell().add(new Paragraph(value)));
    }

    private void addSummaryRow(Table table, String label, String value, DeviceRgb color) {
        table.addCell(new Cell().add(new Paragraph(label)));
        table.addCell(new Cell().add(new Paragraph(value).setFontColor(color).setBold()));
    }
}
