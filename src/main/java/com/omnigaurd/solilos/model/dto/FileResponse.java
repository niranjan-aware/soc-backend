package com.omnigaurd.solilos.model.dto;

import com.omnigaurd.solilos.model.entity.File;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {
    private Long id;
    private Long scanId;
    private String filePath;
    private String language;
    private Integer linesOfCode;
    private Long fileSizeBytes;
    private Integer issueCount;

    public static FileResponse from(File file, Integer issueCount) {
        return new FileResponse(
            file.getId(),
            file.getScan().getId(),
            file.getFilePath(),
            file.getLanguage(),
            file.getLinesOfCode(),
            file.getFileSizeBytes(),
            issueCount
        );
    }
}
