package com.example.LearnAI.document.controller;

import com.example.LearnAI.document.dto.DocumentStatusResponse;
import com.example.LearnAI.document.dto.UploadResponse;
import com.example.LearnAI.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {

        Long userId = (Long) authentication.getPrincipal();
        UploadResponse response = documentService.uploadDocument(file, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<DocumentStatusResponse> getStatus(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        DocumentStatusResponse response = documentService.getDocumentStatus(id, userId);
        return ResponseEntity.ok(response);
    }
}
