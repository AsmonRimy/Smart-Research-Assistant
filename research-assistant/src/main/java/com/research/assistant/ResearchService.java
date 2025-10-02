package com.research.assistant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
public class ResearchService {

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ResearchService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
        this.objectMapper = objectMapper;
    }

    public String processContent(ResearchRequest request) {
        String prompt = buildPrompt(request);

        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        try {
            String response = webClient.post()
                    .uri(geminiApiUrl + "?key=" + geminiApiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractTextFromResponse(response);

        } catch (Exception e) {
            return "API Error: " + e.getMessage();
        }
    }

    private String extractTextFromResponse(String response) {
        try {
            GeminiResponse geminiResponse = objectMapper.readValue(response, GeminiResponse.class);
            if (geminiResponse.getCandidates() != null && !geminiResponse.getCandidates().isEmpty()) {
                GeminiResponse.Candidate candidate = geminiResponse.getCandidates().get(0);
                if (candidate.getContent() != null &&
                        candidate.getContent().getParts() != null &&
                        !candidate.getContent().getParts().isEmpty()) {
                    return candidate.getContent().getParts().get(0).getText();
                }
            }
            return "No valid content found in response.";
        } catch (Exception e) {
            return "Response Parsing Error: " + e.getMessage();
        }
    }

    private String buildPrompt(ResearchRequest request) {
        StringBuilder prompt = new StringBuilder();
        switch (request.getOperation()) {
            case "summarize":
                prompt.append("Summarize the following content briefly:\n\n");
                break;
            case "suggest":
                prompt.append("Based on the following content, suggest related topics and further reading:\n\n");
                break;
            default:
                throw new IllegalArgumentException("Unknown operation: " + request.getOperation());
        }
        prompt.append(request.getContent());
        return prompt.toString();
    }
}
