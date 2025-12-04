package com.omnigaurd.solilos.service.github;

import com.omnigaurd.solilos.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitCloneService {

    private final FileStorageService fileStorageService;

    public CloneResult cloneRepository(String repoUrl, Long scanId, String branch) {
        CloneResult result = new CloneResult();
        result.setScanId(scanId);
        result.setRepoUrl(repoUrl);

        try {
            // Create scan directory
            if (!fileStorageService.createScanDirectory(scanId)) {
                result.setSuccess(false);
                result.setErrorMessage("Failed to create scan directory");
                return result;
            }

            String clonePath = fileStorageService.getStoragePath(scanId);
            File cloneDir = new File(clonePath);

            log.info("Cloning repository {} to {}", repoUrl, clonePath);

            // Clone repository
            Git git;
            if (branch != null && !branch.isEmpty()) {
                git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(cloneDir)
                    .setBranch(branch)
                    .call();
            } else {
                git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(cloneDir)
                    .call();
            }

            git.close();

            // Collect file information
            result.setClonePath(clonePath);
            result.setSuccess(true);
            
            // Get file statistics
            FileStats stats = collectFileStats(clonePath);
            result.setTotalFiles(stats.getTotalFiles());
            result.setTotalSize(stats.getTotalSize());
            result.setFilesByLanguage(stats.getFilesByLanguage());

            log.info("Successfully cloned repository. Total files: {}, Size: {} bytes", 
                stats.getTotalFiles(), stats.getTotalSize());

        } catch (GitAPIException | IOException e) {
            log.error("Failed to clone repository: {}", repoUrl, e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            
            // Cleanup on failure
            fileStorageService.deleteScanDirectory(scanId);
        }

        return result;
    }

    private FileStats collectFileStats(String clonePath) throws IOException {
        FileStats stats = new FileStats();
        Path root = Paths.get(clonePath);

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                .filter(path -> !path.toString().contains("/.git/")) // Ignore .git folder
                .forEach(path -> {
                    try {
                        long size = Files.size(path);
                        stats.addFile(path.toString(), size);
                        
                        // Count by language
                        String extension = getFileExtension(path.toString());
                        stats.incrementLanguageCount(extension);
                    } catch (IOException e) {
                        log.warn("Failed to get size for file: {}", path, e);
                    }
                });
        }

        return stats;
    }

    private String getFileExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filePath.length() - 1) {
            return filePath.substring(lastDot + 1).toLowerCase();
        }
        return "unknown";
    }

    public List<String> listFiles(String clonePath, String extension) throws IOException {
        List<String> files = new ArrayList<>();
        Path root = Paths.get(clonePath);

        if (!Files.exists(root)) {
            return files;
        }

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                .filter(path -> !path.toString().contains("/.git/"))
                .filter(path -> extension == null || path.toString().endsWith("." + extension))
                .forEach(path -> files.add(path.toString()));
        }

        return files;
    }

    // Inner classes
    @lombok.Data
    public static class CloneResult {
        private Long scanId;
        private String repoUrl;
        private String clonePath;
        private boolean success;
        private String errorMessage;
        private int totalFiles;
        private long totalSize;
        private java.util.Map<String, Integer> filesByLanguage;
    }

    @lombok.Data
    private static class FileStats {
        private int totalFiles = 0;
        private long totalSize = 0;
        private java.util.Map<String, Integer> filesByLanguage = new java.util.HashMap<>();

        void addFile(String path, long size) {
            totalFiles++;
            totalSize += size;
        }

        void incrementLanguageCount(String extension) {
            filesByLanguage.merge(extension, 1, Integer::sum);
        }
    }
}
