package com.example.LearnAI.course.service;

public interface LlmService {

    /**
     * Sends the extracted PDF text to an LLM and returns a structured JSON
     * string representing the generated course content.
     */
    String generateCourseContent(String pdfText);
}
