package com.opencode.alumxbackend.jobposts.controller;

import com.opencode.alumxbackend.auth.dto.LoginRequest;
import com.opencode.alumxbackend.auth.dto.LoginResponse;
import com.opencode.alumxbackend.jobposts.dto.CommentRequest;
import com.opencode.alumxbackend.jobposts.dto.PagedPostResponse;
import com.opencode.alumxbackend.jobposts.model.JobPost;
import com.opencode.alumxbackend.jobposts.model.JobPostLike;
import com.opencode.alumxbackend.jobposts.repository.JobPostLikeRepository;
import com.opencode.alumxbackend.jobposts.repository.JobPostRepository;
import com.opencode.alumxbackend.users.model.User;
import com.opencode.alumxbackend.users.model.UserRole;
import com.opencode.alumxbackend.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Post Search & Filtering functionality.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PostSearchIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private JobPostLikeRepository jobPostLikeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User testUser;
    private WebClient webClient;
    private String accessToken;

    @BeforeEach
    void setUp() {
        resetDatabase();

        webClient = WebClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        // Create test user
        testUser = User.builder()
                .username("searchtestuser")
                .email("searchtest@test.com")
                .name("Search Test User")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(UserRole.STUDENT)
                .profileCompleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testUser = userRepository.save(testUser);

        // Login to get access token
        LoginRequest loginRequest = new LoginRequest("searchtest@test.com", "password123");
        LoginResponse loginResponse = webClient.post()
                .uri("/api/auth/login")
                .bodyValue(loginRequest)
                .retrieve()
                .bodyToMono(LoginResponse.class)
                .block();

        assertThat(loginResponse)
                .as("Login should return a token for the freshly created test user")
                .isNotNull();

        accessToken = loginResponse.getAccessToken();

        // Create test posts with different descriptions and dates
        createTestPost("Java Backend Developer position available", 
                LocalDateTime.now().minusDays(10));
        createTestPost("Python Data Science role with machine learning", 
                LocalDateTime.now().minusDays(5));
        createTestPost("Frontend React Developer needed for startup", 
                LocalDateTime.now().minusDays(3));
        createTestPost("Java Full Stack Engineer with Spring Boot experience", 
                LocalDateTime.now().minusDays(1));
        createTestPost("DevOps Engineer with Kubernetes and Docker knowledge", 
                LocalDateTime.now());
    }

    private void createTestPost(String description, LocalDateTime createdAt) {
        JobPost post = JobPost.builder()
                .username(testUser.getUsername())
                .description(description)
                .createdAt(createdAt)
                .build();
        jobPostRepository.save(post);
    }

    /**
     * Ensure each test starts with a clean slate. H2 keeps the same in-memory DB
     * for the cached Spring context, so we have to wipe dependent tables that
     * reference users or job posts before inserting new data.
     */
    private void resetDatabase() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (String table : List.of(
                "chat_read_states",
                "chats",
                "connections",
                "group_chat_participants",
                "group_chats",
                "group_messages",
                "group_read_states",
                "job_post_comments",
                "job_post_images",
                "job_post_likes",
                "job_posts",
                "messages",
                "notifications",
                "resume",
                "user_certifications",
                "user_communication_skills",
                "user_education",
                "user_experience",
                "user_frameworks",
                "user_hobbies",
                "user_internships",
                "user_languages",
                "user_projects",
                "user_skills",
                "user_soft_skills",
                "user_tech_stack",
                "users"
        )) {
            jdbcTemplate.execute("TRUNCATE TABLE " + table);
        }
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    @Test
    void testSearchByKeyword_CaseInsensitive() {
        PagedPostResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/posts/search")
                        .queryParam("keyword", "java")
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(PagedPostResponse.class)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getPosts()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getCurrentPage()).isEqualTo(0);
        assertThat(response.getPosts().get(0).getContent().toLowerCase()).contains("java");
        assertThat(response.getPosts().get(1).getContent().toLowerCase()).contains("java");
    }

    @Test
    void testSearchByKeyword_UpperCase() {
        PagedPostResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/posts/search")
                        .queryParam("keyword", "PYTHON")
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(PagedPostResponse.class)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getPosts()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getPosts().get(0).getContent().toLowerCase()).contains("python");
    }

    @Test
    void testSearchByKeyword_PartialMatch() {
        PagedPostResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/posts/search")
                        .queryParam("keyword", "dev")
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(PagedPostResponse.class)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getPosts().size()).isGreaterThanOrEqualTo(3);
        assertThat(response.getTotalElements()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void testSearchByDateRange() {
        LocalDateTime from = LocalDateTime.now().minusDays(4);
        LocalDateTime to = LocalDateTime.now().plusDays(1);

        PagedPostResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/posts/search")
                        .queryParam("dateFrom", from.toString())
                        .queryParam("dateTo", to.toString())
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(PagedPostResponse.class)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getPosts()).hasSize(3);
        assertThat(response.getTotalElements()).isEqualTo(3);
    }

    @Test
    void testSearchByDateFrom() {
        LocalDateTime from = LocalDateTime.now().minusDays(2);

        PagedPostResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/posts/search")
                        .queryParam("dateFrom", from.toString())
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(PagedPostResponse.class)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getPosts()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
    }

    @Test
    void testSearchByDateTo() {
        LocalDateTime to = LocalDateTime.now().minusDays(4);

        PagedPostResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/posts/search")
                        .queryParam("dateTo", to.toString())
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(PagedPostResponse.class)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getPosts()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
    }

    @Test
    void testSearchWithKeywordAndDateRange() {
        LocalDateTime from = LocalDateTime.now().minusDays(6);
        LocalDateTime to = LocalDateTime.now();

        PagedPostResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/posts/search")
                        .queryParam("keyword", "developer")
                        .queryParam("dateFrom", from.toString())
                        .queryParam("dateTo", to.toString())
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(PagedPostResponse.class)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getPosts().size()).isGreaterThanOrEqualTo(1);
        assertThat(response.getTotalElements()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void testPagination_FirstPage() {
        PagedPostResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/posts/search")
                        .queryParam("page", "0")
                        .queryParam("size", "2")
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(PagedPostResponse.class)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getPosts()).hasSize(2);
        assertThat(response.getCurrentPage()).isEqualTo(0);
        assertThat(response.getPageSize()).isEqualTo(2);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.getTotalElements()).isEqualTo(5);
        assertThat(response.getIsFirst()).isTrue();
        assertThat(response.getIsLast()).isFalse();
        assertThat(response.getHasNext()).isTrue();
        assertThat(response.getHasPrevious()).isFalse();
    }

    @Test
    void testPagination_SecondPage() {
        PagedPostResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/posts/search")
                        .queryParam("page", "1")
                        .queryParam("size", "2")
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(PagedPostResponse.class)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getPosts()).hasSize(2);
        assertThat(response.getCurrentPage()).isEqualTo(1);
        assertThat(response.getPageSize()).isEqualTo(2);
        assertThat(response.getIsFirst()).isFalse();
        assertThat(response.getHasNext()).isTrue();
        assertThat(response.getHasPrevious()).isTrue();
    }

    @Test
    void testPagination_LastPage() {
        PagedPostResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/posts/search")
                        .queryParam("page", "2")
                        .queryParam("size", "2")
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(PagedPostResponse.class)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getPosts()).hasSize(1);
        assertThat(response.getCurrentPage()).isEqualTo(2);
        assertThat(response.getIsLast()).isTrue();
        assertThat(response.getHasNext()).isFalse();
        assertThat(response.getHasPrevious()).isTrue();
    }

    @Test
    void testDefaultPagination() {
        PagedPostResponse response = webClient.get()
                .uri("/api/posts/search")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(PagedPostResponse.class)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getPosts()).hasSize(5);
        assertThat(response.getCurrentPage()).isEqualTo(0);
        assertThat(response.getPageSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(5);
    }

    @Test
    void testSearchNoResults() {
        PagedPostResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/posts/search")
                        .queryParam("keyword", "nonexistentkeyword12345")
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(PagedPostResponse.class)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getPosts()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(0);
        assertThat(response.getTotalPages()).isEqualTo(0);
    }

    @Test
    void testSearchAllPosts_NoFilters() {
        PagedPostResponse response = webClient.get()
                .uri("/api/posts/search")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(PagedPostResponse.class)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getPosts()).hasSize(5);
        assertThat(response.getTotalElements()).isEqualTo(5);
    }

    @Test
    void testPaginationSizeLimit() {
        // Test that size is capped at max (100)
        PagedPostResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/posts/search")
                        .queryParam("page", "0")
                        .queryParam("size", "200")
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(PagedPostResponse.class)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getPageSize()).isEqualTo(100);
    }

    @Test
    void testNegativePageNumber() {
        // Test that negative page numbers default to 0
        PagedPostResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/posts/search")
                        .queryParam("page", "-1")
                        .queryParam("size", "10")
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(PagedPostResponse.class)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getCurrentPage()).isEqualTo(0);
    }

    @Test
    void testResultsOrderedByCreatedAtDesc() {
        PagedPostResponse response = webClient.get()
                .uri("/api/posts/search")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(PagedPostResponse.class)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getPosts()).hasSize(5);
        // Most recent post should be first
        assertThat(response.getPosts().get(0).getContent().toLowerCase()).contains("devops");
        // Oldest post should be last
        assertThat(response.getPosts().get(4).getContent().toLowerCase()).contains("backend");
    }

    @Test
    void testSortByMostLiked() {
        // Create posts and add likes via repository
        JobPost postA = jobPostRepository.save(JobPost.builder()
                .username(testUser.getUsername())
                .description("Most liked Java Spring Boot position")
                .createdAt(LocalDateTime.now().minusHours(3))
                .build());
        JobPost postB = jobPostRepository.save(JobPost.builder()
                .username(testUser.getUsername())
                .description("Moderately liked Python Django role")
                .createdAt(LocalDateTime.now().minusHours(2))
                .build());

        // postA: 5 likes (most)
        for (int i = 0; i < 5; i++) {
            User liker = userRepository.save(User.builder()
                    .username("likera" + System.nanoTime() + "_" + i)
                    .name("Liker A" + i)
                    .email("likera" + System.nanoTime() + "_" + i + "@test.com")
                    .passwordHash("pwd")
                    .role(UserRole.STUDENT)
                    .profileCompleted(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
            jobPostLikeRepository.save(JobPostLike.builder()
                    .jobPost(postA).user(liker).createdAt(LocalDateTime.now()).build());
        }
        // postB: 2 likes
        for (int i = 0; i < 2; i++) {
            User liker = userRepository.save(User.builder()
                    .username("likerb" + System.nanoTime() + "_" + i)
                    .name("Liker B" + i)
                    .email("likerb" + System.nanoTime() + "_" + i + "@test.com")
                    .passwordHash("pwd")
                    .role(UserRole.STUDENT)
                    .profileCompleted(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
            jobPostLikeRepository.save(JobPostLike.builder()
                    .jobPost(postB).user(liker).createdAt(LocalDateTime.now()).build());
        }

        PagedPostResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/posts/search")
                        .queryParam("sortBy", "most_liked")
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(PagedPostResponse.class)
                .block();

        assertThat(response).isNotNull();
        // postA (5 likes) should be first, postB (2 likes) should be second
        assertThat(response.getPosts().get(0).getId()).isEqualTo(postA.getPostId());
        assertThat(response.getPosts().get(0).getLikeCount()).isEqualTo(5);
        assertThat(response.getPosts().get(1).getId()).isEqualTo(postB.getPostId());
        assertThat(response.getPosts().get(1).getLikeCount()).isEqualTo(2);
    }

    @Test
    void testSortByMostCommented() {
        // Create posts with comments via API
        JobPost postA = jobPostRepository.save(JobPost.builder()
                .username(testUser.getUsername())
                .description("Highly commented Java microservices position available now for experienced developers who want to work on cutting edge technology")
                .createdAt(LocalDateTime.now().minusHours(3))
                .build());
        JobPost postB = jobPostRepository.save(JobPost.builder()
                .username(testUser.getUsername())
                .description("Single comment Python testing role open for mid level engineers interested in automation frameworks")
                .createdAt(LocalDateTime.now().minusHours(2))
                .build());

        // postA: 3 comments
        for (int i = 0; i < 3; i++) {
            webClient.post()
                    .uri("/api/jobpost/addcomment/" + postA.getPostId())
                    .header("Authorization", "Bearer " + accessToken)
                    .bodyValue(new CommentRequest("Test comment " + i, testUser.getId()))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        }
        // postB: 1 comment
        webClient.post()
                .uri("/api/jobpost/addcomment/" + postB.getPostId())
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(new CommentRequest("One comment here", testUser.getId()))
                .retrieve()
                .toBodilessEntity()
                .block();

        PagedPostResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/posts/search")
                        .queryParam("sortBy", "most_commented")
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(PagedPostResponse.class)
                .block();

        assertThat(response).isNotNull();
        // postA (3 comments) should be first, postB (1 comment) should be second
        assertThat(response.getPosts().get(0).getId()).isEqualTo(postA.getPostId());
        assertThat(response.getPosts().get(0).getCommentCount()).isEqualTo(3);
        assertThat(response.getPosts().get(1).getId()).isEqualTo(postB.getPostId());
        assertThat(response.getPosts().get(1).getCommentCount()).isEqualTo(1);
    }

    @Test
    void testFilterByUsername() {
        // Create second user
        User secondUser = userRepository.save(User.builder()
                .username("otherauthor")
                .email("otherauthor@test.com")
                .name("Other Author")
                .passwordHash("pwd")
                .role(UserRole.ALUMNI)
                .profileCompleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        // Create posts for both users
        jobPostRepository.save(JobPost.builder()
                .username(testUser.getUsername())
                .description("Searchtestuser unique Java backend developer role")
                .createdAt(LocalDateTime.now().minusHours(1))
                .build());
        jobPostRepository.save(JobPost.builder()
                .username(secondUser.getUsername())
                .description("Otherauthor unique Python data science opening available")
                .createdAt(LocalDateTime.now())
                .build());

        // Filter by secondUser's username
        PagedPostResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/posts/search")
                        .queryParam("username", secondUser.getUsername())
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(PagedPostResponse.class)
                .block();

        assertThat(response).isNotNull();
        // Should only contain posts by secondUser
        assertThat(response.getPosts()).allMatch(
                p -> p.getUsername().equals(secondUser.getUsername()));
        assertThat(response.getPosts()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void testCombinedSortAndFilter() {
        User filterUser = userRepository.save(User.builder()
                .username("comboauthor")
                .email("comboauthor@test.com")
                .name("Combo Author")
                .passwordHash("pwd")
                .role(UserRole.ALUMNI)
                .profileCompleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        // Create posts by filterUser
        JobPost userPost1 = jobPostRepository.save(JobPost.builder()
                .username(filterUser.getUsername())
                .description("Combo Java Spring Boot microservices position for senior engineers")
                .createdAt(LocalDateTime.now().minusHours(2))
                .build());
        JobPost userPost2 = jobPostRepository.save(JobPost.builder()
                .username(filterUser.getUsername())
                .description("Combo Python Django web development role for mid level")
                .createdAt(LocalDateTime.now().minusHours(1))
                .build());

        // Create post by another user (should be filtered out)
        jobPostRepository.save(JobPost.builder()
                .username(testUser.getUsername())
                .description("Combo Java position by different author that should not appear")
                .createdAt(LocalDateTime.now())
                .build());

        // Add more likes to userPost1
        for (int i = 0; i < 3; i++) {
            User liker = userRepository.save(User.builder()
                    .username("comboliker" + System.nanoTime() + "_" + i)
                    .name("Combo Liker " + i)
                    .email("comboliker" + System.nanoTime() + "_" + i + "@test.com")
                    .passwordHash("pwd")
                    .role(UserRole.STUDENT)
                    .profileCompleted(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
            jobPostLikeRepository.save(JobPostLike.builder()
                    .jobPost(userPost1).user(liker).createdAt(LocalDateTime.now()).build());
        }

        // Sort by most liked + filter by username
        PagedPostResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/posts/search")
                        .queryParam("sortBy", "most_liked")
                        .queryParam("username", filterUser.getUsername())
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(PagedPostResponse.class)
                .block();

        assertThat(response).isNotNull();
        // Should only contain filterUser's posts
        assertThat(response.getPosts()).allMatch(
                p -> p.getUsername().equals(filterUser.getUsername()));
        // userPost1 (3 likes) should be first
        assertThat(response.getPosts().get(0).getId()).isEqualTo(userPost1.getPostId());
        assertThat(response.getPosts().get(0).getLikeCount()).isEqualTo(3);
    }
}
