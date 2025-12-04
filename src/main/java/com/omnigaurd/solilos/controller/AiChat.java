package com.omnigaurd.solilos.controller;

import com.omnigaurd.solilos.model.ChatRequest;
import com.omnigaurd.solilos.model.ChatResponse;
import com.omnigaurd.solilos.service.ContextEnricher;
import com.omnigaurd.solilos.service.GroqService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AiChat {

    private final GroqService groqService;
    private final ContextEnricher contextEnricher;

    public AiChat(GroqService groqService, ContextEnricher contextEnricher) {
        this.groqService = groqService;
        this.contextEnricher = contextEnricher;
    }

    @GetMapping("/health-legacy")
    public String health() {
        return "Working fine";
    }

    @PostMapping("/ai-chat")
    public ChatResponse chatWithAI(@RequestBody ChatRequest request) {
        try {
            String enrichedPrompt = contextEnricher.buildEnrichedPrompt(
                    request.getMessage(),
                    request.getSystemPrompt(),
                    request.getCodebaseContext()
            );

            System.out.println("Enriched Prompt Length: " + enrichedPrompt.length());

            String content = groqService.chat(enrichedPrompt);

            int inputTokens = estimateTokens(enrichedPrompt);
            int outputTokens = estimateTokens(content);

            return new ChatResponse(
                    content,
                    new ChatResponse.TokenUsage(inputTokens, outputTokens)
            );
        } catch (Exception e) {
            throw new RuntimeException("AI chat failed", e);
        }
    }

    private int estimateTokens(String text) {
        return text.split("\\s+").length;
    }
}
