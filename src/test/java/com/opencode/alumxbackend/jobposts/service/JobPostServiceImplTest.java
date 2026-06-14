package com.opencode.alumxbackend.jobposts.service;

import com.opencode.alumxbackend.common.exception.Errors.ResourceNotFoundException;
import com.opencode.alumxbackend.jobposts.dto.JobPostResponse;
import com.opencode.alumxbackend.jobposts.dto.PagedPostResponse;
import com.opencode.alumxbackend.jobposts.dto.PostSearchRequest;
import com.opencode.alumxbackend.jobposts.model.JobPost;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobPostServiceImplTest {

    @Mock
    private JobPostRepository jobPostRepository;

    @Mock
    private UserRepository userRepository;

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
    }

    @Test
    @DisplayName("searchPosts - default (no sort) uses the latest-ordering query")
    void searchPosts_DefaultSort_UsesLatestQuery() {
        PostSearchRequest request = PostSearchRequest.builder().build();
        Page<JobPost> page = new PageImpl<>(List.of(testPost2, testPost1), PageRequest.of(0, 10), 2);
        when(jobPostRepository.searchPostsLatest(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        PagedPostResponse response = jobPostService.searchPosts(request);

        assertThat(response.getPosts()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getCurrentPage()).isEqualTo(0);
        verify(jobPostRepository).searchPostsLatest(isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("searchPosts - sort=mostLiked uses the most-liked query")
    void searchPosts_MostLiked_UsesMostLikedQuery() {
        PostSearchRequest request = PostSearchRequest.builder().sort("mostLiked").build();
        when(jobPostRepository.searchPostsMostLiked(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testPost1), PageRequest.of(0, 10), 1));

        PagedPostResponse response = jobPostService.searchPosts(request);

        assertThat(response.getPosts()).hasSize(1);
        verify(jobPostRepository).searchPostsMostLiked(isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("searchPosts - sort=mostCommented uses the most-commented query")
    void searchPosts_MostCommented_UsesMostCommentedQuery() {
        PostSearchRequest request = PostSearchRequest.builder().sort("mostCommented").build();
        when(jobPostRepository.searchPostsMostCommented(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testPost1), PageRequest.of(0, 10), 1));

        PagedPostResponse response = jobPostService.searchPosts(request);

        assertThat(response.getPosts()).hasSize(1);
        verify(jobPostRepository).searchPostsMostCommented(isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("searchPosts - unknown sort value falls back to latest")
    void searchPosts_UnknownSort_FallsBackToLatest() {
        PostSearchRequest request = PostSearchRequest.builder().sort("nonsense").build();
        when(jobPostRepository.searchPostsLatest(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<JobPost>(Collections.emptyList(), PageRequest.of(0, 10), 0));

        jobPostService.searchPosts(request);

        verify(jobPostRepository).searchPostsLatest(isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("searchPosts - passes keyword and username filters through to the repository")
    void searchPosts_PassesKeywordAndUsernameFilters() {
        PostSearchRequest request = PostSearchRequest.builder()
                .keyword("java")
                .username("testuser")
                .sort("mostLiked")
                .build();
        when(jobPostRepository.searchPostsMostLiked(eq("java"), eq("testuser"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testPost1), PageRequest.of(0, 10), 1));

        jobPostService.searchPosts(request);

        verify(jobPostRepository)
                .searchPostsMostLiked(eq("java"), eq("testuser"), isNull(), isNull(), any(Pageable.class));
    }
}