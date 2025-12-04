package com.omnigaurd.solilos.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportResponse {
    private int totalFound;
    private int imported;
    private int alreadyExists;
    private List<String> errors;
    private List<RepositoryResponse> repositories;
}
