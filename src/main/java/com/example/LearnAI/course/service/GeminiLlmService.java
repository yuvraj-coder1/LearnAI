package com.example.LearnAI.course.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GeminiLlmService implements LlmService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiLlmService(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String model,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    @Override
    public String generateCourseContent(String pdfText) {
        // Truncate to ~30k chars to stay within token limits
        if (pdfText.length() > 30000) {
            pdfText = pdfText.substring(0, 30000);
        }

        String prompt = buildPrompt(pdfText);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.7
                )
        );

        String responseJson = restClient.post()
                .uri("/models/{model}:generateContent?key={key}", model, apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return extractContent(responseJson);
    }

    private String buildPrompt(String pdfText) {
        return """
                You are an expert course creator. Given the following PDF text content, generate a structured course.

                Return a JSON object with this exact structure:
                {
                  "title": "Course title",
                  "description": "Brief course description (1-2 sentences)",
                  "sections": [
                    {
                      "title": "Section title",
                      "contentBlocks": [
                        {"type": "text", "content": "Main explanation text"},
                        {"type": "key_point", "content": "An important takeaway"},
                        {"type": "example", "content": "A practical example"}
                      ],
                      "quizQuestions": [
                        {
                          "question": "Question text?",
                          "options": ["Option A", "Option B", "Option C", "Option D"],
                          "correctAnswer": 0,
                          "explanation": "Why this answer is correct"
                        }
                      ]
                    }
                  ]
                }

                Rules:
                - Generate 3 to 5 sections based on the content's natural divisions
                - Each section must have 2-4 content blocks of varying types (text, key_point, example)
                - Each section must have 2-3 quiz questions (multiple choice, 4 options each)
                - correctAnswer is the 0-based index of the correct option
                - Content should be educational, clear, and accurate to the source material
                - Do NOT invent information not present in the source text

                PDF Content:
                \"\"\"
                """ + pdfText + """
                \"\"\"
                """;
    }

    private String extractContent(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty()) {
                throw new RuntimeException("Gemini returned no candidates. Response: " + responseJson);
            }
            return candidates.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response", e);
        }
    }
}
