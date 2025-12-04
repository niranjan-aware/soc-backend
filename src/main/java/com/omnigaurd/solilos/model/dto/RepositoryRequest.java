package com.omnigaurd.solilos.model.dto;

import lombok.Data;

@Data
public class RepositoryRequest {
    private String githubUsername;
    private String githubOrg;
    private String type; // "username" or "org"
}
