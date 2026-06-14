package com.opencode.alumxbackend.jobposts.repository;

import com.opencode.alumxbackend.jobposts.dto.PostWithCountsProjection;
import com.opencode.alumxbackend.jobposts.model.JobPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, Long> {
    List<JobPost> findByUsernameOrderByCreatedAtDesc(String username);

    @Query("SELECT p FROM JobPost p WHERE " +
           "(:keyword IS NULL OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:dateFrom IS NULL OR p.createdAt >= :dateFrom) AND " +
           "(:dateTo IS NULL OR p.createdAt <= :dateTo) " +
           "ORDER BY p.createdAt DESC")
    Page<JobPost> searchPosts(
            @Param("keyword") String keyword,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable
    );

    @Query("SELECT p FROM JobPost p WHERE " +
           "(:username IS NULL OR LOWER(p.username) = LOWER(:username)) AND " +
           "(:keyword IS NULL OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:dateFrom IS NULL OR p.createdAt >= :dateFrom) AND " +
           "(:dateTo IS NULL OR p.createdAt <= :dateTo) " +
           "ORDER BY p.createdAt DESC")
    Page<JobPost> searchPostsWithUsername(
            @Param("keyword") String keyword,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("username") String username,
            Pageable pageable
    );

    @Query(value = "SELECT p.post_id AS postId, p.username AS username, " +
           "p.description AS description, p.created_at AS createdAt, " +
           "COUNT(l.id) AS likeCount, 0 AS commentCount " +
           "FROM job_posts p LEFT JOIN job_post_likes l ON p.post_id = l.post_id " +
           "WHERE (:keyword IS NULL OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:dateFrom IS NULL OR p.created_at >= :dateFrom) AND " +
           "(:dateTo IS NULL OR p.created_at <= :dateTo) " +
           "GROUP BY p.post_id, p.username, p.description, p.created_at " +
           "ORDER BY COUNT(l.id) DESC",
           countQuery = "SELECT COUNT(DISTINCT p.post_id) FROM job_posts p " +
           "WHERE (:keyword IS NULL OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:dateFrom IS NULL OR p.created_at >= :dateFrom) AND " +
           "(:dateTo IS NULL OR p.created_at <= :dateTo)",
           nativeQuery = true)
    Page<PostWithCountsProjection> searchPostsMostLiked(
            @Param("keyword") String keyword,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable
    );

    @Query(value = "SELECT p.post_id AS postId, p.username AS username, " +
           "p.description AS description, p.created_at AS createdAt, " +
           "COUNT(l.id) AS likeCount, 0 AS commentCount " +
           "FROM job_posts p LEFT JOIN job_post_likes l ON p.post_id = l.post_id " +
           "WHERE (:username IS NULL OR LOWER(p.username) = LOWER(:username)) AND " +
           "(:keyword IS NULL OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:dateFrom IS NULL OR p.created_at >= :dateFrom) AND " +
           "(:dateTo IS NULL OR p.created_at <= :dateTo) " +
           "GROUP BY p.post_id, p.username, p.description, p.created_at " +
           "ORDER BY COUNT(l.id) DESC",
           countQuery = "SELECT COUNT(DISTINCT p.post_id) FROM job_posts p " +
           "WHERE (:username IS NULL OR LOWER(p.username) = LOWER(:username)) AND " +
           "(:keyword IS NULL OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:dateFrom IS NULL OR p.created_at >= :dateFrom) AND " +
           "(:dateTo IS NULL OR p.created_at <= :dateTo)",
           nativeQuery = true)
    Page<PostWithCountsProjection> searchPostsMostLikedByUsername(
            @Param("keyword") String keyword,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("username") String username,
            Pageable pageable
    );

    @Query(value = "SELECT p.post_id AS postId, p.username AS username, " +
           "p.description AS description, p.created_at AS createdAt, " +
           "0 AS likeCount, COUNT(c.id) AS commentCount " +
           "FROM job_posts p LEFT JOIN job_post_comments c ON p.post_id = c.post_id " +
           "WHERE (:keyword IS NULL OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:dateFrom IS NULL OR p.created_at >= :dateFrom) AND " +
           "(:dateTo IS NULL OR p.created_at <= :dateTo) " +
           "GROUP BY p.post_id, p.username, p.description, p.created_at " +
           "ORDER BY COUNT(c.id) DESC",
           countQuery = "SELECT COUNT(DISTINCT p.post_id) FROM job_posts p " +
           "WHERE (:keyword IS NULL OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:dateFrom IS NULL OR p.created_at >= :dateFrom) AND " +
           "(:dateTo IS NULL OR p.created_at <= :dateTo)",
           nativeQuery = true)
    Page<PostWithCountsProjection> searchPostsMostCommented(
            @Param("keyword") String keyword,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable
    );

    @Query(value = "SELECT p.post_id AS postId, p.username AS username, " +
           "p.description AS description, p.created_at AS createdAt, " +
           "0 AS likeCount, COUNT(c.id) AS commentCount " +
           "FROM job_posts p LEFT JOIN job_post_comments c ON p.post_id = c.post_id " +
           "WHERE (:username IS NULL OR LOWER(p.username) = LOWER(:username)) AND " +
           "(:keyword IS NULL OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:dateFrom IS NULL OR p.created_at >= :dateFrom) AND " +
           "(:dateTo IS NULL OR p.created_at <= :dateTo) " +
           "GROUP BY p.post_id, p.username, p.description, p.created_at " +
           "ORDER BY COUNT(c.id) DESC",
           countQuery = "SELECT COUNT(DISTINCT p.post_id) FROM job_posts p " +
           "WHERE (:username IS NULL OR LOWER(p.username) = LOWER(:username)) AND " +
           "(:keyword IS NULL OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:dateFrom IS NULL OR p.created_at >= :dateFrom) AND " +
           "(:dateTo IS NULL OR p.created_at <= :dateTo)",
           nativeQuery = true)
    Page<PostWithCountsProjection> searchPostsMostCommentedByUsername(
            @Param("keyword") String keyword,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("username") String username,
            Pageable pageable
    );
}
