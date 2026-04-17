package com.example.LearnAI.course.service;

import com.example.LearnAI.auth.entity.User;
import com.example.LearnAI.course.dto.CourseResponse;
import com.example.LearnAI.course.dto.LlmCourseResponse;
import com.example.LearnAI.course.entity.ContentBlock;
import com.example.LearnAI.course.entity.Course;
import com.example.LearnAI.course.entity.Section;
import com.example.LearnAI.course.repository.CourseRepository;
import com.example.LearnAI.document.entity.Document;
import com.example.LearnAI.document.service.PdfTextExtractor;
import com.example.LearnAI.document.service.StorageService;
import com.example.LearnAI.quiz.entity.QuizQuestion;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final StorageService storageService;
    private final PdfTextExtractor pdfTextExtractor;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    public Course generateCourse(Document document, User user) throws IOException {
        // 1. Download PDF from storage and extract text.
        // No @Transactional: the LLM HTTP call below takes 30-60s and must not hold a DB connection.
        String pdfText;
        try (InputStream pdfStream = storageService.download(document.getStorageKey())) {
            pdfText = pdfTextExtractor.extract(pdfStream);
        }

        if (pdfText == null || pdfText.isBlank()) {
            throw new IllegalArgumentException("Could not extract any text from the PDF");
        }

        // 2. Call LLM to generate structured course content
        String llmJson = llmService.generateCourseContent(pdfText);
        LlmCourseResponse llmResponse = objectMapper.readValue(llmJson, LlmCourseResponse.class);

        // 3. Map LLM response to entities and save
        Course course = new Course();
        course.setUser(user);
        course.setDocument(document);
        course.setTitle(truncate(llmResponse.getTitle(), 255));
        course.setDescription(truncate(llmResponse.getDescription(), 255));

        List<LlmCourseResponse.LlmSection> llmSections = llmResponse.getSections();
        for (int i = 0; i < llmSections.size(); i++) {
            LlmCourseResponse.LlmSection llmSection = llmSections.get(i);

            Section section = new Section();
            section.setCourse(course);
            section.setTitle(llmSection.getTitle());
            section.setOrderIndex(i);

            List<ContentBlock> contentBlocks = llmSection.getContentBlocks().stream()
                    .map(cb -> new ContentBlock(cb.getType(), cb.getContent()))
                    .toList();
            section.setContentBlocks(contentBlocks);

            for (LlmCourseResponse.LlmQuizQuestion llmQ : llmSection.getQuizQuestions()) {
                QuizQuestion quiz = new QuizQuestion();
                quiz.setSection(section);
                quiz.setQuestion(llmQ.getQuestion());
                quiz.setOptions(llmQ.getOptions());
                quiz.setCorrectAnswer(llmQ.getCorrectAnswer());
                quiz.setExplanation(llmQ.getExplanation());
                section.getQuizQuestions().add(quiz);
            }

            course.getSections().add(section);
        }

        return courseRepository.save(course);
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourse(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        if (!course.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Course not found");
        }

        return mapToResponse(course);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getUserCourses(Long userId) {
        return courseRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    private CourseResponse mapToResponse(Course course) {
        List<CourseResponse.SectionResponse> sections = course.getSections().stream()
                .map(section -> new CourseResponse.SectionResponse(
                        section.getId(),
                        section.getTitle(),
                        section.getContentBlocks().stream()
                                .map(cb -> new CourseResponse.ContentBlockResponse(cb.getType(), cb.getContent()))
                                .toList(),
                        section.getQuizQuestions().stream()
                                .map(q -> new CourseResponse.QuizQuestionResponse(
                                        q.getId(),
                                        q.getQuestion(),
                                        q.getOptions(),
                                        q.getCorrectAnswer(),
                                        q.getExplanation()))
                                .toList()))
                .toList();

        return new CourseResponse(course.getId(), course.getTitle(), course.getDescription(), sections);
    }
}
