package com.opencode.alumxbackend.search.service;

import com.opencode.alumxbackend.users.dto.UserResponseDto;
import com.opencode.alumxbackend.users.model.User;
import com.opencode.alumxbackend.users.model.UserRole;
import com.opencode.alumxbackend.users.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserSearchServiceFilterTest {

    @Autowired
    private UserSearchService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        saveUser("flt_alice", "Alice Filter", "flt.alice@example.com", UserRole.ALUMNI,
                "Google", List.of("Java", "Spring Boot"), List.of("B.Tech CSE 2020"));
        saveUser("flt_bob", "Bob Filter", "flt.bob@example.com", UserRole.ALUMNI,
                "Microsoft", List.of("Java", "Python"), List.of("B.Tech ECE 2019"));
        saveUser("flt_carol", "Carol Filter", "flt.carol@example.com", UserRole.STUDENT,
                "Google", List.of("Python"), List.of("M.Tech 2021"));
        saveUser("flt_dave", "Dave Filter", "flt.dave@example.com", UserRole.PROFESSOR,
                null, List.of(), List.of("PhD 2010"));
    }

    private void saveUser(String username, String name, String email, UserRole role,
                          String company, List<String> skills, List<String> education) {
        User user = User.builder()
                .username(username)
                .name(name)
                .email(email)
                .passwordHash(passwordEncoder.encode("password"))
                .role(role)
                .profileCompleted(false)
                .currentCompany(company)
                .skills(skills)
                .education(education)
                .build();
        userRepository.save(user);
    }

    private List<String> emails(List<UserResponseDto> result) {
        return result.stream().map(UserResponseDto::getEmail).toList();
    }

    // ---------- single filters ----------

    @Test
    @DisplayName("Filter by role only is case-insensitive")
    void filterByRoleCaseInsensitive() {
        List<String> emails = emails(service.search(null, "alumni", null, null, null));
        assertThat(emails).contains("flt.alice@example.com", "flt.bob@example.com");
        assertThat(emails).doesNotContain("flt.carol@example.com", "flt.dave@example.com");
    }

    @Test
    @DisplayName("Filter by company is a case-insensitive partial match")
    void filterByCompanyPartialCaseInsensitive() {
        List<String> emails = emails(service.search(null, null, "goo", null, null));
        assertThat(emails).contains("flt.alice@example.com", "flt.carol@example.com");
        assertThat(emails).doesNotContain("flt.bob@example.com", "flt.dave@example.com");
    }

    @Test
    @DisplayName("Filter by a single skill is case-insensitive")
    void filterBySingleSkillCaseInsensitive() {
        List<String> emails = emails(service.search(null, null, null, List.of("java"), null));
        assertThat(emails).contains("flt.alice@example.com", "flt.bob@example.com");
        assertThat(emails).doesNotContain("flt.carol@example.com", "flt.dave@example.com");
    }

    @Test
    @DisplayName("Multiple skills use AND semantics (user must have all of them)")
    void filterByMultipleSkillsAnd() {
        List<String> emails = emails(service.search(null, null, null, List.of("Java", "Python"), null));
        assertThat(emails).contains("flt.bob@example.com");
        assertThat(emails).doesNotContain(
                "flt.alice@example.com", "flt.carol@example.com", "flt.dave@example.com");
    }

    @Test
    @DisplayName("Filter by graduation year matches the education entries")
    void filterByGraduationYear() {
        List<String> emails = emails(service.search(null, null, null, null, 2020));
        assertThat(emails).contains("flt.alice@example.com");
        assertThat(emails).doesNotContain(
                "flt.bob@example.com", "flt.carol@example.com", "flt.dave@example.com");
    }

    // ---------- combinations ----------

    @Test
    @DisplayName("Combining role and company narrows results")
    void filterByRoleAndCompany() {
        List<String> emails = emails(service.search(null, "ALUMNI", "Google", null, null));
        assertThat(emails).containsExactly("flt.alice@example.com");
    }

    @Test
    @DisplayName("Keyword plus role filter narrows results")
    void keywordPlusRoleFilter() {
        // q=goo matches the Google company (alice + carol); role ALUMNI keeps only alice.
        List<String> emails = emails(service.search("goo", "alumni", null, null, null));
        assertThat(emails).containsExactly("flt.alice@example.com");
    }

    @Test
    @DisplayName("Combining a skill and graduation year narrows results")
    void filterBySkillAndGraduationYear() {
        List<String> emails = emails(service.search(null, null, null, List.of("Java"), 2019));
        assertThat(emails).containsExactly("flt.bob@example.com");
    }

    // ---------- empty / blank handling & backward compatibility ----------

    @Test
    @DisplayName("No query and no filters throws IllegalArgumentException")
    void noCriteriaThrows() {
        assertThatThrownBy(() -> service.search(null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Blank query and blank filters throws IllegalArgumentException")
    void blankCriteriaThrows() {
        assertThatThrownBy(() -> service.search("   ", "   ", "   ", List.of("  "), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Blank filters are ignored when a query is present")
    void blankFiltersIgnoredWithQuery() {
        List<String> emails = emails(service.search("flt_bob", "  ", "  ", List.of("  "), null));
        assertThat(emails).contains("flt.bob@example.com");
    }

    @Test
    @DisplayName("Query-only search preserves the original keyword behaviour")
    void queryOnlyPreservesOriginalBehavior() {
        List<String> emails = emails(service.search("flt_alice"));
        assertThat(emails).contains("flt.alice@example.com");
    }

    @Test
    @DisplayName("Invalid role value throws IllegalArgumentException")
    void invalidRoleThrows() {
        assertThatThrownBy(() -> service.search(null, "MANAGER", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
