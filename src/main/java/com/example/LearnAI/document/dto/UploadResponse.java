package com.example.LearnAI.document.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UploadResponse {
    private Long documentId;
    private Long courseId;
    private String message;
}
