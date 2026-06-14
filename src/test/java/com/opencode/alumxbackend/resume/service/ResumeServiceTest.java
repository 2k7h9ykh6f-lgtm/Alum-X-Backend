package com.opencode.alumxbackend.resume.service;

import com.opencode.alumxbackend.common.exception.Errors.InvalidResumeException;
import com.opencode.alumxbackend.notifications.dto.NotificationRequest;
import com.opencode.alumxbackend.notifications.service.NotificationService;
import com.opencode.alumxbackend.resume.dto.ResumeResponseDto;
import com.opencode.alumxbackend.resume.model.Resume;
import com.opencode.alumxbackend.resume.repository.ResumeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ResumeService resumeService;

    @TempDir
    File tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resumeService, "uploadDir", tempDir.getAbsolutePath());
    }

    private MultipartFile mockPdfFile(long sizeBytes) {
        byte[] content = new byte[(int) sizeBytes];
        return new MockMultipartFile("file", "my_resume.pdf", "application/pdf", content);
    }

    private void stubSaveWithId() {
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });
    }

    @Test
    @DisplayName("uploadResume - new upload creates RESUME_UPDATED notification with 'uploaded' message")
    void uploadResume_newUpload_createsNotification() throws Exception {
        Long userId = 1L;
        MultipartFile file = mockPdfFile(1024);

        when(resumeRepository.findByUserId(userId)).thenReturn(Optional.empty());
        stubSaveWithId();

        ResumeResponseDto result = resumeService.uploadResume(userId, file);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getFileName()).isEqualTo("my_resume.pdf");
        assertThat(result.getFileType()).isEqualTo("application/pdf");
        assertThat(result.getFileSize()).isEqualTo(1024L);
        assertThat(result.getUpdatedAt()).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).createNotification(captor.capture());

        NotificationRequest notification = captor.getValue();
        assertThat(notification.getUserId()).isEqualTo(userId);
        assertThat(notification.getType()).isEqualTo("RESUME_UPDATED");
        assertThat(notification.getMessage()).contains("uploaded");
        assertThat(notification.getReferenceId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("uploadResume - replacement creates RESUME_UPDATED notification with 'updated' message")
    void uploadResume_replacement_createsNotification() throws Exception {
        Long userId = 2L;
        MultipartFile file = mockPdfFile(2048);

        Resume existingResume = Resume.builder()
                .id(50L)
                .userId(userId)
                .fileName("old_resume.pdf")
                .fileType("application/pdf")
                .fileUrl(tempDir.getAbsolutePath() + "/" + userId + "_resume.pdf")
                .fileSize(1000L)
                .uploadedAt(LocalDateTime.now().minusDays(7))
                .isActive(true)
                .isDeleted(false)
                .build();

        when(resumeRepository.findByUserId(userId)).thenReturn(Optional.of(existingResume));
        stubSaveWithId();

        ResumeResponseDto result = resumeService.uploadResume(userId, file);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getFileSize()).isEqualTo(2048L);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).createNotification(captor.capture());

        NotificationRequest notification = captor.getValue();
        assertThat(notification.getUserId()).isEqualTo(userId);
        assertThat(notification.getType()).isEqualTo("RESUME_UPDATED");
        assertThat(notification.getMessage()).contains("updated");
        assertThat(notification.getReferenceId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("uploadResume - illegal file type throws exception and does NOT create notification")
    void uploadResume_illegalFileType_noNotification() {
        Long userId = 3L;
        byte[] content = new byte[1024];
        MultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", content);

        assertThatThrownBy(() -> resumeService.uploadResume(userId, file))
                .isInstanceOf(InvalidResumeException.class)
                .hasMessageContaining("Only PDF and DOCX");

        verify(notificationService, never()).createNotification(any());
        verify(resumeRepository, never()).save(any());
    }

    @Test
    @DisplayName("uploadResume - file too large throws exception and does NOT create notification")
    void uploadResume_fileTooLarge_noNotification() {
        Long userId = 4L;
        long sixMb = 6 * 1024 * 1024;
        MultipartFile file = mockPdfFile(sixMb);

        assertThatThrownBy(() -> resumeService.uploadResume(userId, file))
                .isInstanceOf(InvalidResumeException.class)
                .hasMessageContaining("5MB");

        verify(notificationService, never()).createNotification(any());
        verify(resumeRepository, never()).save(any());
    }
}
