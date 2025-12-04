package com.omnigaurd.solilos.service;

import com.omnigaurd.solilos.model.CodebaseContext;
import org.springframework.stereotype.Service;

@Service
public class ContextEnricher {

    public String buildEnrichedPrompt(String message, String systemPrompt, CodebaseContext codebaseContext) {
        StringBuilder enrichedPrompt = new StringBuilder();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            enrichedPrompt.append(systemPrompt).append("\n\n");
        }

        if (codebaseContext != null) {
            enrichedPrompt.append(buildCodebaseContextSection(codebaseContext));
        }

        enrichedPrompt.append("User Message: ").append(message);

        return enrichedPrompt.toString();
    }

    private String buildCodebaseContextSection(CodebaseContext context) {
        StringBuilder section = new StringBuilder();

        section.append("=== CODEBASE CONTEXT ===\n\n");

        if (context.getMode() != null) {
            section.append("Mode: ").append(context.getMode()).append("\n");
            if (context.getBatchNumber() != null && context.getTotalBatches() != null) {
                section.append("Batch: ").append(context.getBatchNumber())
                        .append("/").append(context.getTotalBatches()).append("\n");
            }
            section.append("\n");
        }

        if (context.getGraph() != null) {
            section.append(buildGraphSection(context.getGraph()));
        }

        if (context.getFiles() != null && !context.getFiles().isEmpty()) {
            section.append(buildFilesSection(context.getFiles()));
        }

        if (context.getConversationContext() != null) {
            section.append(buildConversationContextSection(context.getConversationContext()));
        }

        section.append("=== END CODEBASE CONTEXT ===\n\n");

        return section.toString();
    }

    private String buildGraphSection(CodebaseContext.GraphData graph) {
        StringBuilder section = new StringBuilder();

        section.append("--- DEPENDENCY GRAPH ---\n\n");

        if (graph.getMermaid() != null && !graph.getMermaid().isEmpty()) {
            section.append("Visual Dependency Graph:\n")
                    .append("```mermaid\n")
                    .append(graph.getMermaid())
                    .append("\n```\n\n");
        }

        if (graph.getTree() != null && !graph.getTree().isEmpty()) {
            section.append("Hierarchical Structure:\n")
                    .append("```\n")
                    .append(graph.getTree())
                    .append("\n```\n\n");
        }

        return section.toString();
    }

    private String buildFilesSection(java.util.List<CodebaseContext.FileData> files) {
        StringBuilder section = new StringBuilder();

        section.append("--- FILES IN THIS BATCH ---\n\n");

        for (CodebaseContext.FileData file : files) {
            section.append("File: ").append(file.getPath()).append("\n");
            section.append("Language: ").append(file.getLanguage()).append("\n");
            section.append("Role: ").append(file.getRole()).append("\n");

            if (file.getSummary() != null && !file.getSummary().isEmpty()) {
                section.append("Summary: ").append(file.getSummary()).append("\n");
            }

            if (file.getCode() != null && !file.getCode().isEmpty()) {
                section.append("Code:\n```").append(file.getLanguage()).append("\n")
                        .append(file.getCode())
                        .append("\n```\n");
            }

            if (file.getDependencies() != null) {
                if (file.getDependencies().getImports() != null && !file.getDependencies().getImports().isEmpty()) {
                    section.append("Imports: ").append(String.join(", ", file.getDependencies().getImports())).append("\n");
                }
                if (file.getDependencies().getImportedBy() != null && !file.getDependencies().getImportedBy().isEmpty()) {
                    section.append("Imported By: ").append(String.join(", ", file.getDependencies().getImportedBy())).append("\n");
                }
            }

            section.append("\n---\n\n");
        }

        return section.toString();
    }

    private String buildConversationContextSection(CodebaseContext.ConversationContext convContext) {
        StringBuilder section = new StringBuilder();

        section.append("--- CONVERSATION CONTEXT ---\n\n");

        if (convContext.getPreviousBatches() != null && !convContext.getPreviousBatches().isEmpty()) {
            section.append("Previous Batches Processed: ")
                    .append(convContext.getPreviousBatches())
                    .append("\n");
        }

        if (convContext.getKeyTakeaways() != null && !convContext.getKeyTakeaways().isEmpty()) {
            section.append("Key Takeaways:\n");
            for (String takeaway : convContext.getKeyTakeaways()) {
                section.append("- ").append(takeaway).append("\n");
            }
        }

        if (convContext.getUserGoal() != null && !convContext.getUserGoal().isEmpty()) {
            section.append("User Goal: ").append(convContext.getUserGoal()).append("\n");
        }

        section.append("\n");

        return section.toString();
    }
}
