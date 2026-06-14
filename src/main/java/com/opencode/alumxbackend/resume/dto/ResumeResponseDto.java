package com.opencode.alumxbackend.resume.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ResumeResponseDto {
    private Long id;
    private Long userId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private LocalDateTime updatedAt;
}
