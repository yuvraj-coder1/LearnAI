package com.example.LearnAI.course.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class LlmCourseResponse {
    private String title;
    private String description;
    private List<LlmSection> sections;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class LlmSection {
        private String title;
        private List<LlmContentBlock> contentBlocks;
        private List<LlmQuizQuestion> quizQuestions;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class LlmContentBlock {
        private String type;
        private String content;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class LlmQuizQuestion {
        private String question;
        private List<String> options;
        private int correctAnswer;
        private String explanation;
    }
}
