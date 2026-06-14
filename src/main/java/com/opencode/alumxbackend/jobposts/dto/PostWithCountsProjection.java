package com.opencode.alumxbackend.jobposts.dto;

import java.time.LocalDateTime;

/**
 * Interface projection for native queries that return post data
 * along with aggregated like/comment counts.
 */
public interface PostWithCountsProjection {
    Long getPostId();
    String getUsername();
    String getDescription();
    LocalDateTime getCreatedAt();
    Long getLikeCount();
    Long getCommentCount();
}
