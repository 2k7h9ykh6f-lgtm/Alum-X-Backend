package com.opencode.alumxbackend.jobposts.repository;

import com.opencode.alumxbackend.jobposts.model.JobPost;
import com.opencode.alumxbackend.jobposts.model.JobPostComment;
import com.opencode.alumxbackend.jobposts.model.JobPostLike;
import com.opencode.alumxbackend.users.model.User;
import com.opencode.alumxbackend.users.model.UserRole;
import com.opencode.alumxbackend.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the engagement-aware feed queries on {@link JobPostRepository}.
 *
 * <p>Uses the project's standard {@code @SpringBootTest} + {@code test} profile (H2)
 * setup. {@code @Transactional} rolls each test back, so the queries run against a
 * known, isolated data set. Three posts are arranged so the latest / most-liked /
 * most-commented orderings are all distinct, which lets each test prove the correct
 * query is doing the ordering. Filtering by author username and by keyword is also
 * exercised.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JobPostRepositoryTest {

    private static final Pageable FIRST_PAGE = PageRequest.of(0, 10);

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private JobPostLikeRepository jobPostLikeRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    private JobPost postAliceOldest;
    private JobPost postAliceMiddle;
    private JobPost postBobNewest;

    @BeforeEach
    void setUp() {
        User alice = saveUser("alice", "alice@test.com");
        User bob = saveUser("bob", "bob@test.com");
        User carol = saveUser("carol", "carol@test.com");

        LocalDateTime now = LocalDateTime.now();
        postAliceOldest = savePost("alice", "Alice shares her Java backend journey", now.minusDays(3));
        postAliceMiddle = savePost("alice", "Alice on Python data science", now.minusDays(2));
        postBobNewest = savePost("bob", "Bob talks about Java frontend", now.minusDays(1));

        // Likes: middle = 3, oldest = 1, newest = 0  -> mostLiked: middle, oldest, newest
        saveLike(postAliceMiddle, alice);
        saveLike(postAliceMiddle, bob);
        saveLike(postAliceMiddle, carol);
        saveLike(postAliceOldest, alice);

        // Comments: newest = 2, oldest = 1, middle = 0 -> mostCommented: newest, oldest, middle
        saveComment(postBobNewest, alice, "Great write-up");
        saveComment(postBobNewest, bob, "Thanks for sharing");
        saveComment(postAliceOldest, carol, "Very helpful");
    }

    @Test
    @DisplayName("searchPostsLatest - orders by createdAt descending")
    void searchPostsLatest_OrdersByCreatedAtDesc() {
        Page<JobPost> page = jobPostRepository.searchPostsLatest(null, null, null, null, FIRST_PAGE);

        assertThat(page.getContent()).extracting(JobPost::getPostId)
                .containsExactly(postBobNewest.getPostId(), postAliceMiddle.getPostId(), postAliceOldest.getPostId());
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("searchPostsMostLiked - orders by like count descending")
    void searchPostsMostLiked_OrdersByLikeCountDesc() {
        Page<JobPost> page = jobPostRepository.searchPostsMostLiked(null, null, null, null, FIRST_PAGE);

        assertThat(page.getContent()).extracting(JobPost::getPostId)
                .containsExactly(postAliceMiddle.getPostId(), postAliceOldest.getPostId(), postBobNewest.getPostId());
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("searchPostsMostCommented - orders by comment count descending")
    void searchPostsMostCommented_OrdersByCommentCountDesc() {
        Page<JobPost> page = jobPostRepository.searchPostsMostCommented(null, null, null, null, FIRST_PAGE);

        assertThat(page.getContent()).extracting(JobPost::getPostId)
                .containsExactly(postBobNewest.getPostId(), postAliceOldest.getPostId(), postAliceMiddle.getPostId());
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("username filter - returns only the given author's posts")
    void filterByUsername_ReturnsOnlyAuthorsPosts() {
        Page<JobPost> page = jobPostRepository.searchPostsLatest(null, "alice", null, null, FIRST_PAGE);

        assertThat(page.getContent()).extracting(JobPost::getUsername).containsOnly("alice");
        assertThat(page.getContent()).extracting(JobPost::getPostId)
                .containsExactly(postAliceMiddle.getPostId(), postAliceOldest.getPostId());
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("username filter - is case insensitive")
    void filterByUsername_IsCaseInsensitive() {
        Page<JobPost> page = jobPostRepository.searchPostsLatest(null, "ALICE", null, null, FIRST_PAGE);

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(JobPost::getUsername).containsOnly("alice");
    }

    @Test
    @DisplayName("keyword filter - matches description case insensitively")
    void filterByKeyword_MatchesDescriptionCaseInsensitive() {
        Page<JobPost> page = jobPostRepository.searchPostsLatest("JAVA", null, null, null, FIRST_PAGE);

        assertThat(page.getContent()).extracting(JobPost::getPostId)
                .containsExactlyInAnyOrder(postAliceOldest.getPostId(), postBobNewest.getPostId());
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("username + keyword filters combine with AND")
    void filterByUsernameAndKeyword_CombinesWithAnd() {
        Page<JobPost> page = jobPostRepository.searchPostsLatest("java", "alice", null, null, FIRST_PAGE);

        assertThat(page.getContent()).extracting(JobPost::getPostId)
                .containsExactly(postAliceOldest.getPostId());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("pagination - respects page size while reporting full totals")
    void pagination_RespectsPageSizeAndTotals() {
        Page<JobPost> page = jobPostRepository.searchPostsLatest(null, null, null, null, PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }

    private User saveUser(String username, String email) {
        User user = User.builder()
                .username(username)
                .name(username)
                .email(email)
                .passwordHash("hashed")
                .role(UserRole.ALUMNI)
                .profileCompleted(false)
                .build();
        return userRepository.save(user);
    }

    private JobPost savePost(String username, String description, LocalDateTime createdAt) {
        JobPost post = JobPost.builder()
                .username(username)
                .description(description)
                .createdAt(createdAt)
                .build();
        return jobPostRepository.save(post);
    }

    private void saveLike(JobPost post, User user) {
        JobPostLike like = JobPostLike.builder()
                .jobPost(post)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();
        jobPostLikeRepository.save(like);
    }

    private void saveComment(JobPost post, User user, String content) {
        JobPostComment comment = JobPostComment.builder()
                .jobPost(post)
                .user(user)
                .content(content)
                .build();
        commentRepository.save(comment);
    }
}
