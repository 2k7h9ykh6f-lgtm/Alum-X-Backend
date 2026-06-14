package com.opencode.alumxbackend.jobposts.repository;

import com.opencode.alumxbackend.jobposts.dto.PostWithCountsProjection;
import com.opencode.alumxbackend.jobposts.model.JobPost;
import com.opencode.alumxbackend.jobposts.model.JobPostComment;
import com.opencode.alumxbackend.jobposts.model.JobPostLike;
import com.opencode.alumxbackend.users.model.User;
import com.opencode.alumxbackend.users.model.UserRole;
import com.opencode.alumxbackend.users.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JobPostRepositoryTest {

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobPostLikeRepository jobPostLikeRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User testUser;
    private User testUser2;

    @BeforeEach
    void setUp() {
        resetDatabase();

        testUser = User.builder()
                .username("repotestuser")
                .name("Repo Test User")
                .email("repotest" + System.nanoTime() + "@test.com")
                .passwordHash("hashedpwd")
                .role(UserRole.ALUMNI)
                .profileCompleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testUser = userRepository.saveAndFlush(testUser);

        testUser2 = User.builder()
                .username("repotestuser2")
                .name("Repo Test User 2")
                .email("repotest2" + System.nanoTime() + "@test.com")
                .passwordHash("hashedpwd")
                .role(UserRole.STUDENT)
                .profileCompleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testUser2 = userRepository.saveAndFlush(testUser2);
    }

    private void resetDatabase() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (String table : List.of(
                "chat_read_states", "chats", "connections",
                "group_chat_participants", "group_chats", "group_messages", "group_read_states",
                "job_post_comments", "job_post_images", "job_post_likes", "job_posts",
                "messages", "notifications", "resume",
                "user_certifications", "user_communication_skills", "user_education",
                "user_experience", "user_frameworks", "user_hobbies", "user_internships",
                "user_languages", "user_projects", "user_skills", "user_soft_skills",
                "user_tech_stack", "users"
        )) {
            jdbcTemplate.execute("TRUNCATE TABLE " + table);
        }
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    private JobPost createPost(String username, String description, LocalDateTime createdAt) {
        return jobPostRepository.saveAndFlush(JobPost.builder()
                .username(username)
                .description(description)
                .createdAt(createdAt)
                .build());
    }

    private User createLiker(String prefix, int index) {
        return userRepository.saveAndFlush(User.builder()
                .username(prefix + System.nanoTime() + "_" + index)
                .name("Liker " + index)
                .email(prefix + System.nanoTime() + "_" + index + "@test.com")
                .passwordHash("pwd")
                .role(UserRole.STUDENT)
                .profileCompleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("searchPostsMostLiked returns posts ordered by like count DESC")
    void searchPostsMostLiked_ordersCorrectly() {
        JobPost post1 = createPost(testUser.getUsername(), "Java developer position available",
                LocalDateTime.now().minusDays(3));
        JobPost post2 = createPost(testUser.getUsername(), "Python data science role open",
                LocalDateTime.now().minusDays(2));
        JobPost post3 = createPost(testUser.getUsername(), "React frontend developer needed",
                LocalDateTime.now().minusDays(1));

        // post1: 3 likes, post2: 1 like, post3: 0 likes
        for (int i = 0; i < 3; i++) {
            User liker = createLiker("likera_", i);
            jobPostLikeRepository.saveAndFlush(JobPostLike.builder()
                    .jobPost(post1).user(liker).createdAt(LocalDateTime.now()).build());
        }
        User likerForPost2 = createLiker("likerb_", 0);
        jobPostLikeRepository.saveAndFlush(JobPostLike.builder()
                .jobPost(post2).user(likerForPost2).createdAt(LocalDateTime.now()).build());

        entityManager.flush();
        entityManager.clear();

        Page<PostWithCountsProjection> result = jobPostRepository.searchPostsMostLiked(
                null, null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).getPostId()).isEqualTo(post1.getPostId());
        assertThat(result.getContent().get(0).getLikeCount()).isEqualTo(3L);
        assertThat(result.getContent().get(1).getPostId()).isEqualTo(post2.getPostId());
        assertThat(result.getContent().get(1).getLikeCount()).isEqualTo(1L);
        assertThat(result.getContent().get(2).getPostId()).isEqualTo(post3.getPostId());
        assertThat(result.getContent().get(2).getLikeCount()).isEqualTo(0L);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("searchPostsMostCommented returns posts ordered by comment count DESC")
    void searchPostsMostCommented_ordersCorrectly() {
        JobPost post1 = createPost(testUser.getUsername(), "Java backend developer position",
                LocalDateTime.now().minusDays(3));
        JobPost post2 = createPost(testUser.getUsername(), "Python data analyst opening",
                LocalDateTime.now().minusDays(2));
        JobPost post3 = createPost(testUser.getUsername(), "React frontend engineer needed",
                LocalDateTime.now().minusDays(1));

        // post1: 2 comments, post2: 0, post3: 1
        for (int i = 0; i < 2; i++) {
            commentRepository.saveAndFlush(JobPostComment.builder()
                    .jobPost(post1).user(testUser).content("Comment " + i).build());
        }
        commentRepository.saveAndFlush(JobPostComment.builder()
                .jobPost(post3).user(testUser).content("Nice opportunity").build());

        entityManager.flush();
        entityManager.clear();

        Page<PostWithCountsProjection> result = jobPostRepository.searchPostsMostCommented(
                null, null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).getPostId()).isEqualTo(post1.getPostId());
        assertThat(result.getContent().get(0).getCommentCount()).isEqualTo(2L);
        assertThat(result.getContent().get(1).getPostId()).isEqualTo(post3.getPostId());
        assertThat(result.getContent().get(1).getCommentCount()).isEqualTo(1L);
        assertThat(result.getContent().get(2).getPostId()).isEqualTo(post2.getPostId());
        assertThat(result.getContent().get(2).getCommentCount()).isEqualTo(0L);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("searchPostsWithUsername filters by username")
    void searchPostsWithUsername_filtersCorrectly() {
        createPost(testUser.getUsername(), "Java developer position available here",
                LocalDateTime.now().minusDays(2));
        createPost(testUser2.getUsername(), "Python data science role available",
                LocalDateTime.now().minusDays(1));

        entityManager.flush();
        entityManager.clear();

        Page<JobPost> result = jobPostRepository.searchPostsWithUsername(
                null, null, null, testUser.getUsername(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo(testUser.getUsername());
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("searchPostsWithUsername also applies keyword filter")
    void searchPostsWithUsername_appliesKeyword() {
        createPost(testUser.getUsername(), "Java developer position available",
                LocalDateTime.now().minusDays(2));
        createPost(testUser.getUsername(), "Python data science role open",
                LocalDateTime.now().minusDays(1));

        entityManager.flush();
        entityManager.clear();

        Page<JobPost> result = jobPostRepository.searchPostsWithUsername(
                "java", null, null, testUser.getUsername(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDescription().toLowerCase()).contains("java");
    }

    @Test
    @DisplayName("searchPostsMostLikedByUsername combines username and like sort")
    void searchPostsMostLikedByUsername_combinesFilters() {
        JobPost userPost = createPost(testUser.getUsername(), "Java backend developer needed",
                LocalDateTime.now().minusDays(2));
        createPost(testUser2.getUsername(), "Python data scientist opening",
                LocalDateTime.now().minusDays(1));

        User liker = createLiker("comboliker_", 0);
        jobPostLikeRepository.saveAndFlush(JobPostLike.builder()
                .jobPost(userPost).user(liker).createdAt(LocalDateTime.now()).build());

        entityManager.flush();
        entityManager.clear();

        Page<PostWithCountsProjection> result = jobPostRepository.searchPostsMostLikedByUsername(
                null, null, null, testUser.getUsername(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPostId()).isEqualTo(userPost.getPostId());
        assertThat(result.getContent().get(0).getLikeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("searchPostsMostLiked returns empty page when no posts match")
    void searchPostsMostLiked_emptyResult() {
        Page<PostWithCountsProjection> result = jobPostRepository.searchPostsMostLiked(
                "nonexistentkeyword12345", null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("searchPostsMostLiked pagination totalElements is correct with keyword")
    void searchPostsMostLiked_paginationTotalsCorrect() {
        createPost(testUser.getUsername(), "Java senior developer position",
                LocalDateTime.now().minusDays(3));
        createPost(testUser.getUsername(), "Java junior developer position",
                LocalDateTime.now().minusDays(2));
        createPost(testUser.getUsername(), "Python data engineer opening",
                LocalDateTime.now().minusDays(1));

        entityManager.flush();
        entityManager.clear();

        Page<PostWithCountsProjection> result = jobPostRepository.searchPostsMostLiked(
                "java", null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }
}
