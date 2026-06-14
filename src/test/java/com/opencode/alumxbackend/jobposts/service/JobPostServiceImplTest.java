package com.opencode.alumxbackend.jobposts.service;

import com.opencode.alumxbackend.common.exception.Errors.ResourceNotFoundException;
import com.opencode.alumxbackend.jobposts.dto.*;
import com.opencode.alumxbackend.jobposts.model.JobPost;
import com.opencode.alumxbackend.jobposts.repository.CommentRepository;
import com.opencode.alumxbackend.jobposts.repository.JobPostLikeRepository;
import com.opencode.alumxbackend.jobposts.repository.JobPostRepository;
import com.opencode.alumxbackend.users.model.User;
import com.opencode.alumxbackend.users.model.UserRole;
import com.opencode.alumxbackend.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobPostServiceImplTest {

    @Mock
    private JobPostRepository jobPostRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobPostLikeRepository jobPostLikeRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private JobPostServiceImpl jobPostService;

    private User testUser;
    private JobPost testPost1;
    private JobPost testPost2;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .role(UserRole.ALUMNI)
                .profileCompleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testPost1 = JobPost.builder()
                .username("testuser")
                .description("This is the first test job post description")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        testPost1.setPostId(1L);

        testPost2 = JobPost.builder()
                .username("testuser")
                .description("This is the second test job post description")
                .createdAt(LocalDateTime.now())
                .build();

        testPost2.setPostId(2L);
    }

    @Test
    @DisplayName("getPostsByUser - returns posts when user exists and has posts")
    void getPostsByUser_ReturnsPostsWhenUserExistsAndHasPosts() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(jobPostRepository.findByUsernameOrderByCreatedAtDesc("testuser"))
                .thenReturn(List.of(testPost2, testPost1));

        List<JobPostResponse> result = jobPostService.getPostsByUser(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(2L);
        assertThat(result.get(1).getId()).isEqualTo(1L);
        assertThat(result.get(0).getContent()).isEqualTo("This is the second test job post description");
    }

    @Test
    @DisplayName("getPostsByUser - returns empty list when user exists but has no posts")
    void getPostsByUser_ReturnsEmptyListWhenUserHasNoPosts() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(jobPostRepository.findByUsernameOrderByCreatedAtDesc("testuser"))
                .thenReturn(Collections.emptyList());

        List<JobPostResponse> result = jobPostService.getPostsByUser(1L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getPostsByUser - throws ResourceNotFoundException when user does not exist")
    void getPostsByUser_ThrowsExceptionWhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobPostService.getPostsByUser(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("getPostsByUser - response contains all required fields")
    void getPostsByUser_ResponseContainsAllRequiredFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(jobPostRepository.findByUsernameOrderByCreatedAtDesc("testuser"))
                .thenReturn(List.of(testPost1));

        List<JobPostResponse> result = jobPostService.getPostsByUser(1L);

        assertThat(result).hasSize(1);
        JobPostResponse response = result.get(0);
        assertThat(response.getId()).isNotNull();
        assertThat(response.getTitle()).isNotNull();
        assertThat(response.getContent()).isNotNull();
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getLikeCount()).isEqualTo(0);
        assertThat(response.getCommentCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("searchPosts - default sort calls searchPosts repository method")
    void searchPosts_defaultSort_callsSearchPosts() {
        PostSearchRequest request = PostSearchRequest.builder().build();
        Page<JobPost> page = new PageImpl<>(List.of(testPost1));
        when(jobPostRepository.searchPosts(any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        PagedPostResponse result = jobPostService.searchPosts(request);

        assertThat(result.getPosts()).hasSize(1);
        verify(jobPostRepository).searchPosts(any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("searchPosts - latest sort explicitly calls searchPosts")
    void searchPosts_latestSort_callsSearchPosts() {
        PostSearchRequest request = PostSearchRequest.builder().sortBy("latest").build();
        Page<JobPost> page = new PageImpl<>(List.of(testPost1));
        when(jobPostRepository.searchPosts(any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        PagedPostResponse result = jobPostService.searchPosts(request);

        assertThat(result.getPosts()).hasSize(1);
        verify(jobPostRepository).searchPosts(any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("searchPosts - mostLiked sort calls searchPostsMostLiked")
    void searchPosts_mostLiked_callsMostLiked() {
        PostSearchRequest request = PostSearchRequest.builder().sortBy("most_liked").build();

        PostWithCountsProjection projection = createMockProjection(1L, "testuser", "desc", 5L, 0L);
        Page<PostWithCountsProjection> page = new PageImpl<>(List.of(projection));
        when(jobPostRepository.searchPostsMostLiked(any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        PagedPostResponse result = jobPostService.searchPosts(request);

        assertThat(result.getPosts()).hasSize(1);
        assertThat(result.getPosts().get(0).getLikeCount()).isEqualTo(5);
        verify(jobPostRepository).searchPostsMostLiked(any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("searchPosts - mostCommented sort calls searchPostsMostCommented")
    void searchPosts_mostCommented_callsMostCommented() {
        PostSearchRequest request = PostSearchRequest.builder().sortBy("most_commented").build();

        PostWithCountsProjection projection = createMockProjection(1L, "testuser", "desc", 0L, 3L);
        Page<PostWithCountsProjection> page = new PageImpl<>(List.of(projection));
        when(jobPostRepository.searchPostsMostCommented(any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        PagedPostResponse result = jobPostService.searchPosts(request);

        assertThat(result.getPosts()).hasSize(1);
        assertThat(result.getPosts().get(0).getCommentCount()).isEqualTo(3);
        verify(jobPostRepository).searchPostsMostCommented(any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("searchPosts - with username calls username-variant method for latest")
    void searchPosts_withUsername_latest() {
        PostSearchRequest request = PostSearchRequest.builder()
                .sortBy("latest").username("john").build();
        Page<JobPost> page = new PageImpl<>(List.of(testPost1));
        when(jobPostRepository.searchPostsWithUsername(any(), any(), any(), eq("john"), any(Pageable.class)))
                .thenReturn(page);

        PagedPostResponse result = jobPostService.searchPosts(request);

        assertThat(result.getPosts()).hasSize(1);
        verify(jobPostRepository).searchPostsWithUsername(any(), any(), any(), eq("john"), any(Pageable.class));
    }

    @Test
    @DisplayName("searchPosts - mostLiked with username calls username-variant method")
    void searchPosts_mostLikedWithUsername() {
        PostSearchRequest request = PostSearchRequest.builder()
                .sortBy("most_liked").username("john").build();

        PostWithCountsProjection projection = createMockProjection(1L, "john", "desc", 2L, 0L);
        Page<PostWithCountsProjection> page = new PageImpl<>(List.of(projection));
        when(jobPostRepository.searchPostsMostLikedByUsername(any(), any(), any(), eq("john"), any(Pageable.class)))
                .thenReturn(page);

        PagedPostResponse result = jobPostService.searchPosts(request);

        assertThat(result.getPosts()).hasSize(1);
        verify(jobPostRepository).searchPostsMostLikedByUsername(any(), any(), any(), eq("john"), any(Pageable.class));
    }

    @Test
    @DisplayName("searchPosts - invalid sortBy defaults to latest")
    void searchPosts_invalidSortBy_defaultsToLatest() {
        PostSearchRequest request = PostSearchRequest.builder().sortBy("invalid_sort").build();
        Page<JobPost> page = new PageImpl<>(List.of(testPost1));
        when(jobPostRepository.searchPosts(any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        PagedPostResponse result = jobPostService.searchPosts(request);

        assertThat(result.getPosts()).hasSize(1);
        verify(jobPostRepository).searchPosts(any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("searchPosts - null sortBy defaults to latest")
    void searchPosts_nullSortBy_defaultsToLatest() {
        PostSearchRequest request = PostSearchRequest.builder().sortBy(null).build();
        Page<JobPost> page = new PageImpl<>(List.of(testPost1));
        when(jobPostRepository.searchPosts(any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        PagedPostResponse result = jobPostService.searchPosts(request);

        assertThat(result.getPosts()).hasSize(1);
        verify(jobPostRepository).searchPosts(any(), any(), any(), any(Pageable.class));
    }

    private PostWithCountsProjection createMockProjection(Long postId, String username,
                                                          String description, Long likeCount, Long commentCount) {
        return new PostWithCountsProjection() {
            @Override public Long getPostId() { return postId; }
            @Override public String getUsername() { return username; }
            @Override public String getDescription() { return description; }
            @Override public LocalDateTime getCreatedAt() { return LocalDateTime.now(); }
            @Override public Long getLikeCount() { return likeCount; }
            @Override public Long getCommentCount() { return commentCount; }
        };
    }
}
