package com.example.LearnAI.document.service;

import com.example.LearnAI.auth.entity.User;
import com.example.LearnAI.auth.repository.UserRepository;
import com.example.LearnAI.course.entity.Course;
import com.example.LearnAI.course.repository.CourseRepository;
import com.example.LearnAI.course.service.CourseService;
import com.example.LearnAI.document.dto.DocumentStatusResponse;
import com.example.LearnAI.document.dto.UploadResponse;
import com.example.LearnAI.document.entity.Document;
import com.example.LearnAI.document.repository.DocumentRepository;
import com.example.LearnAI.processing.entity.JobStatus;
import com.example.LearnAI.processing.entity.ProcessingJob;
import com.example.LearnAI.processing.repository.ProcessingJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ProcessingJobRepository processingJobRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final CourseService courseService;
    private final CourseRepository courseRepository;

    public UploadResponse uploadDocument(MultipartFile file, Long userId) throws IOException {
        validatePdf(file);

        String fileHash = computeSha256(file.getInputStream());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Per-user dedup: same user uploading the same file again
        Optional<Document> existing = documentRepository.findByFileHashAndUserId(fileHash, userId);
        if (existing.isPresent()) {
            Document existingDoc = existing.get();
            Optional<Course> existingCourse = courseRepository.findByDocumentId(existingDoc.getId());
            if (existingCourse.isPresent()) {
                return new UploadResponse(existingDoc.getId(), existingCourse.get().getId(),
                        "Duplicate file detected, returning existing course");
            }
            // Previous attempt failed or never produced a course — retry generation on the same document
            ProcessingJob job = processingJobRepository.findByDocumentId(existingDoc.getId())
                    .orElseGet(() -> {
                        ProcessingJob j = new ProcessingJob();
                        j.setDocument(existingDoc);
                        return j;
                    });
            job.setStatus(JobStatus.PROCESSING);
            job.setErrorMessage(null);
            job = processingJobRepository.save(job);
            return runCourseGeneration(existingDoc, user, job);
        }

        // Skip storage upload if anyone already uploaded this file (bytes are identical)
        Optional<Document> anyWithSameHash = documentRepository.findFirstByFileHash(fileHash);
        String storageKey;
        if (anyWithSameHash.isPresent()) {
            storageKey = anyWithSameHash.get().getStorageKey();
        } else {
            storageKey = "documents/" + UUID.randomUUID() + ".pdf";
            storageService.upload(storageKey, file.getInputStream(), file.getSize(), "application/pdf");
        }

        // Save document record
        Document document = new Document();
        document.setUser(user);
        document.setFileName(file.getOriginalFilename());
        document.setFileHash(fileHash);
        document.setFileSize(file.getSize());
        document.setStorageKey(storageKey);
        document = documentRepository.save(document);

        // Create processing job
        ProcessingJob job = new ProcessingJob();
        job.setDocument(document);
        job.setStatus(JobStatus.PROCESSING);
        job = processingJobRepository.save(job);

        return runCourseGeneration(document, user, job);
    }

    private UploadResponse runCourseGeneration(Document document, User user, ProcessingJob job) {
        try {
            Course course = courseService.generateCourse(document, user);
            job.setStatus(JobStatus.COMPLETED);
            processingJobRepository.save(job);
            return new UploadResponse(document.getId(), course.getId(), "Course generated successfully");
        } catch (Exception e) {
            log.error("Course generation failed for document {}", document.getId(), e);
            job.setStatus(JobStatus.FAILED);
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 1000) {
                errorMsg = errorMsg.substring(0, 1000);
            }
            job.setErrorMessage(errorMsg);
            processingJobRepository.save(job);
            throw new RuntimeException("Course generation failed: " + e.getMessage(), e);
        }
    }

    public DocumentStatusResponse getDocumentStatus(Long documentId, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        if (!document.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Document not found");
        }

        ProcessingJob job = processingJobRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new IllegalStateException("Job not found for document"));

        Long courseId = courseRepository.findByDocumentId(documentId)
                .map(Course::getId).orElse(null);
        return new DocumentStatusResponse(job.getStatus().name(), courseId);
    }

    private void validatePdf(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Check actual file magic bytes, not just Content-Type header (which clients can spoof)
        byte[] header = new byte[5];
        try (InputStream is = file.getInputStream()) {
            if (is.read(header) < 5) {
                throw new IllegalArgumentException("File is too small to be a valid PDF");
            }
        }
        // Every PDF starts with "%PDF-"
        if (header[0] != 0x25 || header[1] != 0x50 || header[2] != 0x44 || header[3] != 0x46 || header[4] != 0x2D) {
            throw new IllegalArgumentException("Only PDF files are accepted");
        }
    }

    private String computeSha256(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available in every JVM
            throw new RuntimeException(e);
        }
    }
}
