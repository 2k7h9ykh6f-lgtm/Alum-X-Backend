package com.opencode.alumxbackend.search.service;

import com.opencode.alumxbackend.search.dto.UserSearchRequest;
import com.opencode.alumxbackend.users.dto.UserResponseDto;
import com.opencode.alumxbackend.users.model.User;
import com.opencode.alumxbackend.users.model.UserRole;
import com.opencode.alumxbackend.users.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserSearchServiceTest {

    @Autowired
    private UserSearchService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup(){

        User user = User.builder()
                .username("hasan")
                .name("Hasan Ravda")
                .email("hasan@test.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(UserRole.STUDENT)
                .profileCompleted(false)
                .build();

        userRepository.save(user);

        User user1 = User.builder()
                .username("Gaurav")
                .name("Gaurav Chhetri")
                .email("ife2022004@iiita.ac.in")
                .passwordHash(passwordEncoder.encode("password"))
                .role(UserRole.STUDENT)
                .profileCompleted(false)
                .build();

        userRepository.save(user1);


    }

    @Test
    @DisplayName("Service Layer TEst : shouold return correct user from the DB by username")
    void searchByUsername() {

        List<UserResponseDto> result = service.search("ga");

        assertFalse(result.isEmpty());
        System.out.println("=====================================================");
        System.out.println(result);
    }

    // ---- Helper to create users with rich profile data ----
    private User createRichUser(String username, String name, String email,
                                 UserRole role, String company,
                                 List<String> skills, Integer graduationYear) {
        return User.builder()
                .username(username)
                .name(name)
                .email(email)
                .passwordHash(passwordEncoder.encode("password"))
                .role(role)
                .profileCompleted(true)
                .currentCompany(company)
                .skills(skills)
                .graduationYear(graduationYear)
                .build();
    }

    @Nested
    @DisplayName("Filter Search Tests")
    class FilterSearchTests {

        @BeforeEach
        void setupFilterTestData() {
            userRepository.save(createRichUser(
                    "alice_eng", "Alice Engineer", "alice_filter@test.com",
                    UserRole.ALUMNI, "Google",
                    List.of("Java", "Spring", "Kubernetes"), 2020));

            userRepository.save(createRichUser(
                    "bob_dev", "Bob Developer", "bob_filter@test.com",
                    UserRole.ALUMNI, "Microsoft",
                    List.of("Python", "Django", "React"), 2019));

            userRepository.save(createRichUser(
                    "charlie_intern", "Charlie Intern", "charlie_filter@test.com",
                    UserRole.STUDENT, "Google",
                    List.of("Java", "React"), 2025));

            userRepository.save(createRichUser(
                    "diana_prof", "Diana Professor", "diana_filter@test.com",
                    UserRole.PROFESSOR, "University",
                    List.of("Research", "Machine Learning"), 2010));
        }

        @Test
        @DisplayName("Should filter by role only")
        void filterByRole() {
            UserSearchRequest request = UserSearchRequest.builder()
                    .query("a") // broad enough to match multiple
                    .role(UserRole.ALUMNI)
                    .build();

            List<UserResponseDto> result = service.search(request);

            assertFalse(result.isEmpty());
            result.forEach(u -> assertEquals(UserRole.ALUMNI, u.getRole()));
        }

        @Test
        @DisplayName("Should filter by currentCompany (case-insensitive)")
        void filterByCurrentCompany() {
            UserSearchRequest request = UserSearchRequest.builder()
                    .query("a")
                    .currentCompany("google")
                    .build();

            List<UserResponseDto> result = service.search(request);

            assertFalse(result.isEmpty());
            assertTrue(result.size() >= 2); // Alice and Charlie both at Google
        }

        @Test
        @DisplayName("Should filter by skills")
        void filterBySkills() {
            UserSearchRequest request = UserSearchRequest.builder()
                    .query("a")
                    .skills(List.of("Java"))
                    .build();

            List<UserResponseDto> result = service.search(request);

            assertFalse(result.isEmpty());
            // Alice and Charlie both have Java
            assertTrue(result.size() >= 2);
        }

        @Test
        @DisplayName("Should filter by graduationYear")
        void filterByGraduationYear() {
            UserSearchRequest request = UserSearchRequest.builder()
                    .query("a")
                    .graduationYear(2020)
                    .build();

            List<UserResponseDto> result = service.search(request);

            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            assertEquals("Alice Engineer", result.get(0).getName());
        }

        @Test
        @DisplayName("Should combine role + currentCompany filters")
        void filterByRoleAndCompany() {
            UserSearchRequest request = UserSearchRequest.builder()
                    .query("a")
                    .role(UserRole.ALUMNI)
                    .currentCompany("Google")
                    .build();

            List<UserResponseDto> result = service.search(request);

            assertEquals(1, result.size());
            assertEquals("Alice Engineer", result.get(0).getName());
        }

        @Test
        @DisplayName("Should combine role + skills + graduationYear filters")
        void filterByRoleSkillsAndGradYear() {
            UserSearchRequest request = UserSearchRequest.builder()
                    .query("a")
                    .role(UserRole.ALUMNI)
                    .skills(List.of("Java"))
                    .graduationYear(2020)
                    .build();

            List<UserResponseDto> result = service.search(request);

            assertEquals(1, result.size());
            assertEquals("Alice Engineer", result.get(0).getName());
        }

        @Test
        @DisplayName("Should combine all filters and return empty when no match")
        void filterAllNoMatch() {
            UserSearchRequest request = UserSearchRequest.builder()
                    .query("a")
                    .role(UserRole.PROFESSOR)
                    .currentCompany("Google")
                    .skills(List.of("Java"))
                    .graduationYear(2020)
                    .build();

            List<UserResponseDto> result = service.search(request);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should use filters without keyword query")
        void filterWithoutQuery() {
            UserSearchRequest request = UserSearchRequest.builder()
                    .role(UserRole.ALUMNI)
                    .currentCompany("Microsoft")
                    .build();

            List<UserResponseDto> result = service.search(request);

            assertEquals(1, result.size());
            assertEquals("Bob Developer", result.get(0).getName());
        }

        @Test
        @DisplayName("Should handle case-insensitive skills matching")
        void filterSkillsCaseInsensitive() {
            UserSearchRequest request = UserSearchRequest.builder()
                    .skills(List.of("java", "python"))
                    .build();

            List<UserResponseDto> result = service.search(request);

            // Alice (Java), Bob (Python), Charlie (Java) should match
            assertTrue(result.size() >= 3);
        }

        @Test
        @DisplayName("Should handle case-insensitive company matching")
        void filterCompanyCaseInsensitive() {
            UserSearchRequest request = UserSearchRequest.builder()
                    .currentCompany("GOOGLE")
                    .build();

            List<UserResponseDto> result = service.search(request);

            assertTrue(result.size() >= 2);
        }

        @Test
        @DisplayName("Should return empty for non-existent graduation year")
        void filterByNonExistentGraduationYear() {
            UserSearchRequest request = UserSearchRequest.builder()
                    .graduationYear(1900)
                    .build();

            List<UserResponseDto> result = service.search(request);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should throw when request is null")
        void nullRequestThrows() {
            assertThrows(IllegalArgumentException.class, () -> service.search((UserSearchRequest) null));
        }

        @Test
        @DisplayName("Should throw when no query and no filters provided")
        void emptyRequestThrows() {
            UserSearchRequest request = UserSearchRequest.builder().build();
            assertThrows(IllegalArgumentException.class, () -> service.search(request));
        }

        @Test
        @DisplayName("Original search(String) still works unchanged")
        void originalSearchStillWorks() {
            List<UserResponseDto> result = service.search("ga");
            assertFalse(result.isEmpty());
        }
    }
}
