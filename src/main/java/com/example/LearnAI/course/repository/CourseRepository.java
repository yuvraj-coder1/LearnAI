package com.example.LearnAI.course.repository;

import com.example.LearnAI.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByDocumentId(Long documentId);

    List<Course> findByUserIdOrderByCreatedAtDesc(Long userId);
}
