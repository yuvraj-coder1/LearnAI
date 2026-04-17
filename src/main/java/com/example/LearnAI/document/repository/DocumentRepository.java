package com.example.LearnAI.document.repository;

import com.example.LearnAI.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByFileHashAndUserId(String fileHash, Long userId);

    Optional<Document> findFirstByFileHash(String fileHash);
}
