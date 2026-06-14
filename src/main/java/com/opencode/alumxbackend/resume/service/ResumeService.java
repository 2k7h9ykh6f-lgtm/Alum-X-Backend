package com.opencode.alumxbackend.resume.service;

import com.opencode.alumxbackend.common.exception.Errors.InvalidResumeException;
import com.opencode.alumxbackend.common.exception.Errors.ResumeNotFoundException;
import com.opencode.alumxbackend.notifications.dto.NotificationRequest;
import com.opencode.alumxbackend.notifications.service.NotificationService;
import com.opencode.alumxbackend.resume.dto.ResumeResponseDto;
import com.opencode.alumxbackend.resume.model.Resume;
import com.opencode.alumxbackend.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ResumeService {
    private final ResumeRepository resumeRepository;
    private final NotificationService notificationService;

    @Value("${resume.upload.dir}")
    private String uploadDir;

    public ResumeResponseDto uploadResume(Long userId, MultipartFile file) throws Exception {

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new InvalidResumeException("File size must be less than 5MB");
        }

        String contentType = file.getContentType();
        if (!("application/pdf".equals(contentType)
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType))) {
            throw new InvalidResumeException("Only PDF and DOCX files are allowed");
        }

        File directory = new File(uploadDir);
        if (!directory.exists()) directory.mkdirs();

        String extension = contentType.equals("application/pdf") ? ".pdf" : ".docx";
        String filePath = uploadDir + "/" + userId + "_resume" + extension;

        boolean isReplacement = resumeRepository.findByUserId(userId).isPresent();

        resumeRepository.findByUserId(userId).ifPresent(old -> {
            File oldFile = new File(old.getFileUrl());
            if (oldFile.exists()) oldFile.delete();
        });

        Files.write(new File(filePath).toPath(), file.getBytes());

        Resume resume = Resume.builder()
                .userId(userId)
                .fileName(file.getOriginalFilename())
                .fileType(contentType)
                .fileUrl(filePath)
                .fileSize(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .isActive(true)
                .isDeleted(false)
                .build();

        resume = resumeRepository.save(resume);

        String message = isReplacement
                ? "Your resume has been updated."
                : "Your resume has been uploaded successfully.";

        notificationService.createNotification(NotificationRequest.builder()
                .userId(userId)
                .type("RESUME_UPDATED")
                .message(message)
                .referenceId(resume.getId())
                .build());

        return ResumeResponseDto.builder()
                .id(resume.getId())
                .userId(resume.getUserId())
                .fileName(resume.getFileName())
                .fileType(resume.getFileType())
                .fileSize(resume.getFileSize())
                .updatedAt(resume.getUploadedAt())
                .build();
    }

    public Resume getResumeByUserId(Long userId) {
        return resumeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResumeNotFoundException("Resume not found"));
    }
}
