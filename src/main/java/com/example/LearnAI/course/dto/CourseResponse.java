package com.example.LearnAI.course.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {
    private Long id;
    private String title;
    private String description;
    private List<SectionResponse> sections;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionResponse {
        private Long id;
        private String title;
        private List<ContentBlockResponse> contentBlocks;
        private List<QuizQuestionResponse> quizQuestions;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentBlockResponse {
        private String type;
        private String content;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizQuestionResponse {
        private Long id;
        private String question;
        private List<String> options;
        private int correctAnswer;
        private String explanation;
    }
}
