package com.example.LearnAI.processing.repository;

import com.example.LearnAI.processing.entity.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, Long> {

    Optional<ProcessingJob> findByDocumentId(Long documentId);
}
