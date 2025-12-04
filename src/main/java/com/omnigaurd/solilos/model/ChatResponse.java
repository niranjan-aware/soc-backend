package com.omnigaurd.solilos.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String content;
    private TokenUsage tokens;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenUsage {
        private int input;
        private int output;
        private int total;

        public TokenUsage(int input, int output) {
            this.input = input;
            this.output = output;
            this.total = input + output;
        }
    }
}
