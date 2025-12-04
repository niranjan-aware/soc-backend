package com.omnigaurd.solilos.model;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private String systemPrompt;
    private CodebaseContext codebaseContext;
}
