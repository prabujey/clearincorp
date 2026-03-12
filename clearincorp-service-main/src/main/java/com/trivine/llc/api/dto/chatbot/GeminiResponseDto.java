package com.trivine.llc.api.dto.chatbot;

import lombok.Data;

import java.util.List;

@Data
public class GeminiResponseDto {
    private List<Candidate> candidates;

    @Data
    public static class Candidate {
        private Content content;
    }

    @Data
    public static class Content {
        private List<Part> parts;
        private String role;
    }

    @Data
    public static class Part {
        private String text;
    }

    // Helper method to easily extract the response text
    public String getFirstCandidateText() {
        if (candidates != null && !candidates.isEmpty()) {
            Candidate firstCandidate = candidates.get(0);
            if (firstCandidate.getContent() != null && firstCandidate.getContent().getParts() != null && !firstCandidate.getContent().getParts().isEmpty()) {
                return firstCandidate.getContent().getParts().get(0).getText();
            }
        }
        return "Sorry, I couldn't generate a response.";
    }
}