package com.opencode.alumxbackend.resume.controller;

import com.opencode.alumxbackend.resume.dto.ResumeResponseDto;
import com.opencode.alumxbackend.resume.model.Resume;
import com.opencode.alumxbackend.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping
    public ResponseEntity<ResumeResponseDto> uploadResume(
            @RequestParam Long userId,
            @RequestParam MultipartFile file
    ) throws Exception {
        ResumeResponseDto response = resumeService.uploadResume(userId, file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Void> fetchResume(@PathVariable Long userId) {

        Resume resume = resumeService.getResumeByUserId(userId);

        return ResponseEntity
                .status(302)
                .header(HttpHeaders.LOCATION, resume.getFileUrl())
                .build();
    }

}
