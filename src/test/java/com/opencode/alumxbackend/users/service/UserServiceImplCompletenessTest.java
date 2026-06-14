package com.opencode.alumxbackend.users.service;

import com.opencode.alumxbackend.users.dto.UserProfileResponse;
import com.opencode.alumxbackend.users.model.User;
import com.opencode.alumxbackend.users.model.UserRole;
import com.opencode.alumxbackend.users.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplCompletenessTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User baseUser() {
        return User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashed")
                .role(UserRole.STUDENT)
                .profileCompleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("empty profile returns 0% completeness with 11 missing fields")
    void emptyProfile_returnsZero() {
        User user = baseUser();
        // name is set from baseUser, but we want truly empty — override it
        user.setName(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getUserProfile(1L);

        assertThat(response.getProfileCompleteness()).isEqualTo(0);
        assertThat(response.getMissingFields()).hasSize(11);
        assertThat(response.getProfileCompleted()).isFalse();
    }

    @Test
    @DisplayName("full profile returns 100% completeness with no missing fields")
    void fullProfile_returns100() {
        User user = baseUser();
        user.setName("Test User");
        user.setAbout("About me");
        user.setCurrentCompany("Acme");
        user.setCurrentRole("Engineer");
        user.setLocation("Berlin");
        user.setLinkedinUrl("https://linkedin.com/in/test");
        user.setSkills(List.of("Java"));
        user.setEducation(List.of("BS CS"));
        user.setTechStack(List.of("Spring"));
        user.setExperience(List.of("3 years"));
        user.setProjects(List.of("ProjectX"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getUserProfile(1L);

        assertThat(response.getProfileCompleteness()).isEqualTo(100);
        assertThat(response.getMissingFields()).isEmpty();
        assertThat(response.getProfileCompleted()).isTrue();
    }

    @Test
    @DisplayName("single field filled returns 9% completeness")
    void singleField_returns9() {
        User user = baseUser();
        user.setName("Test User");
        // all other key fields null

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getUserProfile(1L);

        assertThat(response.getProfileCompleteness()).isEqualTo(9);
        assertThat(response.getMissingFields()).hasSize(10);
        assertThat(response.getMissingFields()).doesNotContain("name");
    }

    @Test
    @DisplayName("blank string field counts as missing")
    void blankString_countsAsMissing() {
        User user = baseUser();
        user.setName("   ");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getUserProfile(1L);

        assertThat(response.getMissingFields()).contains("name");
    }

    @Test
    @DisplayName("empty list field counts as missing")
    void emptyList_countsAsMissing() {
        User user = baseUser();
        user.setName("Test User");
        user.setSkills(List.of());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getUserProfile(1L);

        assertThat(response.getMissingFields()).contains("skills");
    }

    @Test
    @DisplayName("5 of 11 fields filled returns 45% completeness")
    void halfProfile_returns45() {
        User user = baseUser();
        user.setName("Test User");
        user.setAbout("About me");
        user.setCurrentCompany("Acme");
        user.setCurrentRole("Engineer");
        user.setLocation("Berlin");
        // 5 filled, 6 missing

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getUserProfile(1L);

        assertThat(response.getProfileCompleteness()).isEqualTo(45);
        assertThat(response.getMissingFields()).hasSize(6);
        assertThat(response.getProfileCompleted()).isFalse();
    }

    @Test
    @DisplayName("10 of 11 fields filled returns 90% and profileCompleted=false")
    void almostFull_returns90() {
        User user = baseUser();
        user.setName("Test User");
        user.setAbout("About me");
        user.setCurrentCompany("Acme");
        user.setCurrentRole("Engineer");
        user.setLocation("Berlin");
        user.setLinkedinUrl("https://linkedin.com/in/test");
        user.setSkills(List.of("Java"));
        user.setEducation(List.of("BS CS"));
        user.setTechStack(List.of("Spring"));
        user.setExperience(List.of("3 years"));
        // projects is missing — 10 of 11

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getUserProfile(1L);

        assertThat(response.getProfileCompleteness()).isEqualTo(90);
        assertThat(response.getMissingFields()).hasSize(1);
        assertThat(response.getMissingFields()).contains("projects");
        assertThat(response.getProfileCompleted()).isFalse();
    }
}
