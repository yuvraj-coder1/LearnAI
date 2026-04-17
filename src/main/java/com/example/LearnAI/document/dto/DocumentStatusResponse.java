package com.example.LearnAI.document.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DocumentStatusResponse {

    private String status;
    private Long courseId;
}
