package com.example.LearnAI.course.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentBlock {
    private String type;  // "text", "key_point", "example"
    private String content;
}
