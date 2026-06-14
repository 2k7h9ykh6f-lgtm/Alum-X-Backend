package com.opencode.alumxbackend.jobposts.repository;

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

    String FILTERS =
            "(:keyword IS NULL OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:username IS NULL OR LOWER(p.username) = LOWER(:username)) AND " +
            "(:dateFrom IS NULL OR p.createdAt >= :dateFrom) AND " +
            "(:dateTo IS NULL OR p.createdAt <= :dateTo)";

    String COUNT_QUERY = "SELECT COUNT(p) FROM JobPost p WHERE " + FILTERS;

    /**
     * Latest first. Preserves the original default ordering (createdAt DESC) so
     * existing callers see no behavioural change.
     */
    @Query(value = "SELECT p FROM JobPost p WHERE " + FILTERS + " ORDER BY p.createdAt DESC",
           countQuery = COUNT_QUERY)
    Page<JobPost> searchPostsLatest(
            @Param("keyword") String keyword,
            @Param("username") String username,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable
    );

    /**
     * Most liked first, breaking ties by recency. The like count is computed with a
     * correlated subquery so the result rows stay one-per-post and pagination/count
     * remain straightforward.
     */
    @Query(value = "SELECT p FROM JobPost p WHERE " + FILTERS +
                   " ORDER BY (SELECT COUNT(l) FROM JobPostLike l WHERE l.jobPost = p) DESC, p.createdAt DESC",
           countQuery = COUNT_QUERY)
    Page<JobPost> searchPostsMostLiked(
            @Param("keyword") String keyword,
            @Param("username") String username,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable
    );

    /**
     * Most commented first, breaking ties by recency.
     */
    @Query(value = "SELECT p FROM JobPost p WHERE " + FILTERS +
                   " ORDER BY (SELECT COUNT(c) FROM JobPostComment c WHERE c.jobPost = p) DESC, p.createdAt DESC",
           countQuery = COUNT_QUERY)
    Page<JobPost> searchPostsMostCommented(
            @Param("keyword") String keyword,
            @Param("username") String username,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable
    );
}
