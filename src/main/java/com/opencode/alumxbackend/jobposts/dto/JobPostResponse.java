package com.opencode.alumxbackend.jobposts.dto;

import com.opencode.alumxbackend.jobposts.model.JobPost;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JobPostResponse {
    private Long id;
    private String title;
    private String content;
    private String username;
    private long likeCount;
    private long commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static JobPostResponse fromEntity(JobPost jobPost) {
        return JobPostResponse.builder()
                .id(jobPost.getPostId())
                .title(jobPost.getUsername() + "'s Job Post")
                .content(jobPost.getDescription())
                .username(jobPost.getUsername())
                .likeCount(0)
                .commentCount(0)
                .createdAt(jobPost.getCreatedAt())
                .updatedAt(jobPost.getCreatedAt())
                .build();
    }

    public static JobPostResponse fromProjection(PostWithCountsProjection p) {
        return JobPostResponse.builder()
                .id(p.getPostId())
                .title(p.getUsername() + "'s Job Post")
                .content(p.getDescription())
                .username(p.getUsername())
                .likeCount(p.getLikeCount() != null ? p.getLikeCount() : 0L)
                .commentCount(p.getCommentCount() != null ? p.getCommentCount() : 0L)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getCreatedAt())
                .build();
    }

    public static List<JobPostResponse> fromEntities(List<JobPost> jobPosts) {
        return jobPosts.stream()
                .map(JobPostResponse::fromEntity)
                .toList();
    }
}
