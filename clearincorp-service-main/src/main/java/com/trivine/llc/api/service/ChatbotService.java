package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.chatbot.GeminiRequestDto;
import com.trivine.llc.api.dto.chatbot.GeminiResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Service
public class ChatbotService {

    private final WebClient webClient;
    private final String geminiApiKey;
    private final ResourceLoader resourceLoader;

    // This will hold the content from your knowledge_base.txt file
    private String knowledgeBase;

    // A simple set of greetings to check against
    private static final Set<String> GREETINGS = Set.of("hi", "hello", "hey", "greetings", "good morning", "good afternoon", "good evening");

    public ChatbotService(WebClient.Builder webClientBuilder,
                          @Value("${gemini.api.url}") String geminiApiUrl,
                          @Value("${gemini.api.key}") String geminiApiKey,
                          ResourceLoader resourceLoader) {
        this.webClient = webClientBuilder.baseUrl(geminiApiUrl).build();
        this.geminiApiKey = geminiApiKey;
        this.resourceLoader = resourceLoader;
    }

    /**
     * This method is automatically called after the service is created.
     * It loads the knowledge base from the file in src/main/resources.
     */
    @PostConstruct
    public void init() {
        try {
            Resource resource = resourceLoader.getResource("classpath:knowledge_base.txt");
            InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
            this.knowledgeBase = FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            // If the file cannot be loaded, the application should not start.
            throw new UncheckedIOException("Failed to read knowledge_base.txt. Please ensure the file exists in src/main/resources.", e);
        }
    }

    public Mono<String> getRagResponse(String userQuery) {
        // --- GREETING HANDLING ---
        // Check if the user's query is a simple greeting.
        if (isGreeting(userQuery)) {
            return Mono.just("Hello! How can I help you with your LLC formation today?");
        }

        // --- RAG IMPLEMENTATION ---
        // 1. Retrieval Phase: Use the entire knowledge base loaded from the file.
        String context = this.knowledgeBase;

        // 2. Augmentation Phase: The prompt is structured to enforce answering only from the context.
        String augmentedPrompt = String.format("""
            You are ClearBot, a helpful and professional AI assistant for the ClearIncorp LLC formation platform.
            Your primary role is to answer questions based *exclusively* on the detailed technical and functional documentation provided in the --- CONTEXT --- section.
            Analyze the user's question and find the most relevant section in the context to construct your answer.
            - **NEVER** use any information outside of the provided context.
            - **ALWAYS** be direct and clear in your response.
            - If the information needed to answer the question is not in the context, you MUST respond with exactly this phrase: "I do not have information on that topic. Please ask a question related to our LLC formation services."
            - Format your answers with markdown for clarity, using bolding and lists where helpful.

            --- CONTEXT ---
            %s
            --- END CONTEXT ---

            USER QUESTION: %s

            ANSWER:
            """, context, userQuery);

        // 3. Generation Phase: Call the Gemini API.
        GeminiRequestDto.Part part = new GeminiRequestDto.Part(augmentedPrompt);
        GeminiRequestDto.Content content = new GeminiRequestDto.Content(List.of(part));
        GeminiRequestDto requestDto = new GeminiRequestDto(List.of(content));

        return this.webClient.post()
                .uri(uriBuilder -> uriBuilder.queryParam("key", this.geminiApiKey).build())
                .bodyValue(requestDto)
                .retrieve()
                .bodyToMono(GeminiResponseDto.class)
                .map(GeminiResponseDto::getFirstCandidateText);
    }

    /**
     * Checks if the user's input is a common greeting.
     * @param query The user's message, converted to lower case and trimmed.
     * @return true if the message is a greeting, false otherwise.
     */
    private boolean isGreeting(String query) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }
        // Normalize the query to handle variations
        String normalizedQuery = query.trim().toLowerCase().replaceAll("[^a-z\\s]", "");
        return GREETINGS.contains(normalizedQuery);
    }
}