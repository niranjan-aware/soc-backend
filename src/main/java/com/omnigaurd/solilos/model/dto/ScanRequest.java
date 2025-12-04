package com.omnigaurd.solilos.model.dto;

import lombok.Data;

@Data
public class ScanRequest {
    private Long repositoryId;
    private String branch; // Optional, defaults to repository's default branch
}
