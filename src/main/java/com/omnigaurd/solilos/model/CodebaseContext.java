package com.omnigaurd.solilos.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class CodebaseContext {
    private String mode;
    private Integer batchNumber;
    private Integer totalBatches;
    private GraphData graph;
    private List<FileData> files;
    private ConversationContext conversationContext;

    @Data
    public static class GraphData {
        private String mermaid;
        private String tree;
        private Map<String, Object> metadata;
    }

    @Data
    public static class FileData {
        private String path;
        private String language;
        private String role;
        private Map<String, Object> summary;
        private String code;
        private Dependencies dependencies;
    }

    @Data
    public static class Dependencies {
        private List<String> imports;
        private List<String> importedBy;
        private List<String> transitive;
    }

    @Data
    public static class ConversationContext {
        private List<Integer> previousBatches;
        private List<String> keyTakeaways;
        private String userGoal;
    }
}
