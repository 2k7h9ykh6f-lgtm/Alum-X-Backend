package com.opencode.alumxbackend.resume.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.opencode.alumxbackend.common.exception.Errors.InvalidResumeException;
import com.opencode.alumxbackend.notifications.dto.NotificationRequest;
import com.opencode.alumxbackend.notifications.service.NotificationService;
import com.opencode.alumxbackend.resume.dto.ResumeResponseDto;
import com.opencode.alumxbackend.resume.model.Resume;
import com.opencode.alumxbackend.resume.repository.ResumeRepository;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ResumeService resumeService;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resumeService, "uploadDir", tempDir.toString());
    }

    // ========== SUCCESS CASES ==========

    @Test
    @DisplayName("uploadResume - valid PDF: stores file with metadata and sends RESUME_UPDATED notification")
    void uploadResume_validPdf_savesResumeAndSendsNotification() throws Exception {
        Long userId = 1L;
        byte[] content = "pdf-file-content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", content);

        when(resumeRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        resumeService.uploadResume(userId, file);

        // File is written to disk
        assertThat(Files.exists(tempDir.resolve(userId + "_resume.pdf"))).isTrue();

        // Resume persisted with the new metadata populated
        ArgumentCaptor<Resume> resumeCaptor = ArgumentCaptor.forClass(Resume.class);
        verify(resumeRepository).save(resumeCaptor.capture());
        Resume savedResume = resumeCaptor.getValue();
        assertThat(savedResume.getFileName()).isEqualTo("resume.pdf");
        assertThat(savedResume.getFileType()).isEqualTo("application/pdf");
        assertThat(savedResume.getFileSize()).isEqualTo((long) content.length);
        assertThat(savedResume.getUploadedAt()).isNotNull();

        // Notification of type RESUME_UPDATED is created for the user
        ArgumentCaptor<NotificationRequest> notificationCaptor =
                ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).createNotification(notificationCaptor.capture());
        NotificationRequest request = notificationCaptor.getValue();
        assertThat(request.getType()).isEqualTo("RESUME_UPDATED");
        assertThat(request.getUserId()).isEqualTo(userId);
        assertThat(request.getReferenceId()).isEqualTo(100L);
        assertThat(request.getMessage()).isNotBlank();
    }

    @Test
    @DisplayName("uploadResume - replacing an existing resume deletes the old file and sends a notification")
    void uploadResume_replacesExistingResume_deletesOldFileAndSendsNotification() throws Exception {
        Long userId = 2L;

        // Existing resume points to a real file that must be removed on replace
        Path oldFilePath = tempDir.resolve(userId + "_resume_old.pdf");
        Files.write(oldFilePath, "old-content".getBytes());
        Resume existing = Resume.builder()
                .id(50L)
                .userId(userId)
                .fileName("old.pdf")
                .fileType("application/pdf")
                .fileUrl(oldFilePath.toString())
                .fileSize(11L)
                .uploadedAt(LocalDateTime.now().minusDays(1))
                .isActive(true)
                .build();

        when(resumeRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume r = invocation.getArgument(0);
            r.setId(51L);
            return r;
        });

        MockMultipartFile newFile = new MockMultipartFile(
                "file", "new.pdf", "application/pdf", "new-content".getBytes());

        resumeService.uploadResume(userId, newFile);

        // Old file deleted, new file written
        assertThat(Files.exists(oldFilePath)).isFalse();
        assertThat(Files.exists(tempDir.resolve(userId + "_resume.pdf"))).isTrue();

        verify(resumeRepository).save(any(Resume.class));

        ArgumentCaptor<NotificationRequest> notificationCaptor =
                ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService, times(1)).createNotification(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getType()).isEqualTo("RESUME_UPDATED");
        assertThat(notificationCaptor.getValue().getUserId()).isEqualTo(userId);
    }

    // ========== FAILURE CASES (no notification expected) ==========

    @Test
    @DisplayName("uploadResume - invalid file type is rejected and no notification is sent")
    void uploadResume_invalidFileType_throwsAndDoesNotNotify() {
        Long userId = 3L;
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain", "not-a-resume".getBytes());

        assertThatThrownBy(() -> resumeService.uploadResume(userId, file))
                .isInstanceOf(InvalidResumeException.class);

        verify(resumeRepository, never()).save(any());
        verify(notificationService, never()).createNotification(any());
    }

    @Test
    @DisplayName("uploadResume - file exceeding 5MB is rejected and no notification is sent")
    void uploadResume_fileTooLarge_throwsAndDoesNotNotify() {
        Long userId = 4L;
        MultipartFile largeFile = mock(MultipartFile.class);
        when(largeFile.getSize()).thenReturn(6L * 1024 * 1024);

        assertThatThrownBy(() -> resumeService.uploadResume(userId, largeFile))
                .isInstanceOf(InvalidResumeException.class);

        verify(resumeRepository, never()).save(any());
        verify(notificationService, never()).createNotification(any());
    }

    // ========== METADATA RETRIEVAL ==========

    @Test
    @DisplayName("getResumeInfo - returns updatedAt, fileName and fileSize metadata")
    void getResumeInfo_returnsMetadata() {
        Long userId = 5L;
        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        Resume resume = Resume.builder()
                .id(70L)
                .userId(userId)
                .fileName("cv.pdf")
                .fileType("application/pdf")
                .fileUrl(tempDir.resolve(userId + "_resume.pdf").toString())
                .fileSize(2048L)
                .uploadedAt(updatedAt)
                .isActive(true)
                .build();
        when(resumeRepository.findByUserId(userId)).thenReturn(Optional.of(resume));

        ResumeResponseDto dto = resumeService.getResumeInfo(userId);

        assertThat(dto.getId()).isEqualTo(70L);
        assertThat(dto.getUserId()).isEqualTo(userId);
        assertThat(dto.getFileName()).isEqualTo("cv.pdf");
        assertThat(dto.getFileSize()).isEqualTo(2048L);
        assertThat(dto.getUpdatedAt()).isEqualTo(updatedAt);
    }
}
