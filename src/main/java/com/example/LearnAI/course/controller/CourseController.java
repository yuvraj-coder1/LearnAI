package com.example.LearnAI.course.controller;

import com.example.LearnAI.course.dto.CourseResponse;
import com.example.LearnAI.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourse(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        CourseResponse response = courseService.getCourse(id, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> getUserCourses(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<CourseResponse> courses = courseService.getUserCourses(userId);
        return ResponseEntity.ok(courses);
    }
}
