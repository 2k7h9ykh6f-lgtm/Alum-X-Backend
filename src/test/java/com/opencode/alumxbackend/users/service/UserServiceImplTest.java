package com.opencode.alumxbackend.users.service;

import com.opencode.alumxbackend.users.dto.UserProfileResponse;
import com.opencode.alumxbackend.users.dto.UserProfileUpdateRequest;
import com.opencode.alumxbackend.users.model.User;
import com.opencode.alumxbackend.users.model.UserRole;
import com.opencode.alumxbackend.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests (no Spring context / no database) for the profile-completion
 * logic added to {@link UserServiceImpl}. Mirrors the Mockito style used by
 * {@code JobPostServiceImplTest}.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    /** Field keys in the exact order {@code UserServiceImpl} reports them. */
    private static final List<String> ALL_KEY_FIELDS = List.of(
            "about", "currentRole", "currentCompany", "location",
            "linkedinUrl", "githubUrl",
            "skills", "education", "techStack", "frameworks",
            "languages", "projects", "certifications");

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("gaurav63")
                .name("Gaurav Chhetri")
                .email("gaurav@example.com")
                .passwordHash("hashed")
                .role(UserRole.STUDENT)
                .profileCompleted(true) // stale dev default; should be recomputed
                .build();
    }

    @Test
    @DisplayName("getUserProfile - empty profile reports 0% and lists every key field as missing")
    void getUserProfile_emptyProfile() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse res = userService.getUserProfile(1L);

        assertThat(res.getProfileCompletionPercentage()).isZero();
        assertThat(res.getProfileCompleted()).isFalse();
        assertThat(res.getMissingFields()).containsExactlyElementsOf(ALL_KEY_FIELDS);
    }

    @Test
    @DisplayName("getUserProfile - fully populated profile reports 100% and no missing fields")
    void getUserProfile_completeProfile() {
        populateAllKeyFields(user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse res = userService.getUserProfile(1L);

        assertThat(res.getProfileCompletionPercentage()).isEqualTo(100);
        assertThat(res.getMissingFields()).isEmpty();
        assertThat(res.getProfileCompleted()).isTrue();
    }

    @Test
    @DisplayName("getUserProfile - partial profile reports rounded percentage and only the empty fields")
    void getUserProfile_partialProfile() {
        // Fill 4 of the 13 key fields.
        user.setAbout("Backend engineer");
        user.setCurrentRole("SDE");
        user.setSkills(List.of("Java"));
        user.setEducation(List.of("IIIT-A"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse res = userService.getUserProfile(1L);

        // 4 / 13 = 30.77% -> rounded to 31
        assertThat(res.getProfileCompletionPercentage()).isEqualTo(31);
        assertThat(res.getProfileCompleted()).isFalse();
        assertThat(res.getMissingFields())
                .doesNotContain("about", "currentRole", "skills", "education")
                .containsExactly("currentCompany", "location", "linkedinUrl", "githubUrl",
                        "techStack", "frameworks", "languages", "projects", "certifications");
    }

    @Test
    @DisplayName("getUserProfile - blank strings and empty lists are not counted as filled")
    void getUserProfile_blankValuesNotCounted() {
        user.setAbout("   ");      // whitespace only -> not filled
        user.setSkills(List.of()); // empty list -> not filled
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse res = userService.getUserProfile(1L);

        assertThat(res.getProfileCompletionPercentage()).isZero();
        assertThat(res.getMissingFields()).contains("about", "skills");
    }

    @Test
    @DisplayName("updateUserProfile - recomputes completion and persists profileCompleted=true once complete")
    void updateUserProfile_recomputesAndPersists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileUpdateRequest req = new UserProfileUpdateRequest();
        req.setAbout("Backend engineer");
        req.setCurrentRole("SDE");
        req.setCurrentCompany("Gromo");
        req.setLocation("Bengaluru");
        req.setLinkedinUrl("https://linkedin.com/in/gaurav");
        req.setGithubUrl("https://github.com/gaurav");
        req.setSkills(List.of("Java"));
        req.setEducation(List.of("IIIT-A"));
        req.setTechStack(List.of("Spring"));
        req.setFrameworks(List.of("Spring Boot"));
        req.setLanguages(List.of("English"));
        req.setProjects(List.of("AlumX"));
        req.setCertifications(List.of("OCP"));

        UserProfileResponse res = userService.updateUserProfile(1L, req);

        assertThat(res.getProfileCompletionPercentage()).isEqualTo(100);
        assertThat(res.getMissingFields()).isEmpty();
        assertThat(res.getProfileCompleted()).isTrue();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isProfileCompleted()).isTrue();
    }

    @Test
    @DisplayName("updateUserProfile - partial update keeps profileCompleted=false and reflects new missing set")
    void updateUserProfile_partialKeepsIncomplete() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileUpdateRequest req = new UserProfileUpdateRequest();
        req.setCurrentCompany("DealShare");

        UserProfileResponse res = userService.updateUserProfile(1L, req);

        assertThat(res.getProfileCompleted()).isFalse();
        assertThat(res.getMissingFields()).doesNotContain("currentCompany");
        assertThat(res.getProfileCompletionPercentage()).isGreaterThan(0).isLessThan(100);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isProfileCompleted()).isFalse();
    }

    @Test
    @DisplayName("updateUserProfile - existing response fields remain present (backward compatible)")
    void updateUserProfile_backwardCompatibleResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileUpdateRequest req = new UserProfileUpdateRequest();
        req.setCurrentCompany("DealShare");

        UserProfileResponse res = userService.updateUserProfile(1L, req);

        assertThat(res.getId()).isEqualTo(1L);
        assertThat(res.getUsername()).isEqualTo("gaurav63");
        assertThat(res.getCurrentCompany()).isEqualTo("DealShare");
        assertThat(res.getProfileCompleted()).isNotNull();
        assertThat(res.getProfileCompletionPercentage()).isNotNull();
        assertThat(res.getMissingFields()).isNotNull();
    }

    private void populateAllKeyFields(User u) {
        u.setAbout("Backend engineer with several years of experience");
        u.setCurrentRole("Senior SDE");
        u.setCurrentCompany("Gromo");
        u.setLocation("Bengaluru");
        u.setLinkedinUrl("https://linkedin.com/in/gaurav");
        u.setGithubUrl("https://github.com/gaurav");
        u.setSkills(List.of("Java", "Spring"));
        u.setEducation(List.of("IIIT-A"));
        u.setTechStack(List.of("Spring Boot", "Postgres"));
        u.setFrameworks(List.of("Spring Boot"));
        u.setLanguages(List.of("English", "Hindi"));
        u.setProjects(List.of("AlumX"));
        u.setCertifications(List.of("Oracle Certified Professional"));
    }
}
