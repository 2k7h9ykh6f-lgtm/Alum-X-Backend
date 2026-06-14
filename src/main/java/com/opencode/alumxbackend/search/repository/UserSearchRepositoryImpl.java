package com.opencode.alumxbackend.search.repository;

import com.opencode.alumxbackend.search.dto.UserSearchRequest;
import com.opencode.alumxbackend.users.dto.UserResponseDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class UserSearchRepositoryImpl implements UserSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<UserResponseDto> searchUsers(String query) {

        String jpql = """
    SELECT DISTINCT new com.opencode.alumxbackend.users.dto.UserResponseDto(
        u.id,
        u.name,
        u.email,
        u.role,
        u.createdAt
    )
    FROM User u
    LEFT JOIN u.education e
    LEFT JOIN u.experience exp
    LEFT JOIN u.internships i
    WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))
       OR LOWER(u.name) LIKE LOWER(CONCAT('%', :q, '%'))
       OR LOWER(u.currentCompany) LIKE LOWER(CONCAT('%', :q, '%'))
       OR LOWER(e) LIKE LOWER(CONCAT('%', :q, '%'))
       OR LOWER(exp) LIKE LOWER(CONCAT('%', :q, '%'))
       OR LOWER(i) LIKE LOWER(CONCAT('%', :q, '%'))
""";


        return entityManager.createQuery(jpql, UserResponseDto.class)
                .setParameter("q", query)
                .getResultList();
    }

    @Override
    public List<UserResponseDto> searchUsers(UserSearchRequest request) {
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT DISTINCT new com.opencode.alumxbackend.users.dto.UserResponseDto(");
        jpql.append("u.id, u.name, u.email, u.role, u.createdAt) ");
        jpql.append("FROM User u ");

        // Only join collections when needed for the query or skills filter
        boolean needsEducationJoin = request.hasQuery();
        boolean needsSkillsJoin = request.hasSkills();

        if (needsEducationJoin) {
            jpql.append("LEFT JOIN u.education e ");
            jpql.append("LEFT JOIN u.experience exp ");
            jpql.append("LEFT JOIN u.internships i ");
        }
        if (needsSkillsJoin) {
            jpql.append("LEFT JOIN u.skills sk ");
        }

        List<String> conditions = new ArrayList<>();

        // Keyword search condition (same as original behavior)
        if (request.hasQuery()) {
            conditions.add("(LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) " +
                    "OR LOWER(u.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
                    "OR LOWER(u.currentCompany) LIKE LOWER(CONCAT('%', :q, '%')) " +
                    "OR LOWER(e) LIKE LOWER(CONCAT('%', :q, '%')) " +
                    "OR LOWER(exp) LIKE LOWER(CONCAT('%', :q, '%')) " +
                    "OR LOWER(i) LIKE LOWER(CONCAT('%', :q, '%')))");
        }

        // Role filter (exact match, case handled by enum)
        if (request.hasRole()) {
            conditions.add("u.role = :role");
        }

        // Current company filter (case-insensitive contains)
        if (request.hasCurrentCompany()) {
            conditions.add("LOWER(u.currentCompany) LIKE LOWER(CONCAT('%', :company, '%'))");
        }

        // Skills filter (case-insensitive, any skill matches)
        if (request.hasSkills()) {
            conditions.add("LOWER(sk) IN :skillsLower");
        }

        // Graduation year filter (exact match)
        if (request.hasGraduationYear()) {
            conditions.add("u.graduationYear = :graduationYear");
        }

        if (!conditions.isEmpty()) {
            jpql.append("WHERE ");
            jpql.append(String.join(" AND ", conditions));
        }

        TypedQuery<UserResponseDto> typedQuery = entityManager.createQuery(jpql.toString(), UserResponseDto.class);

        if (request.hasQuery()) {
            typedQuery.setParameter("q", request.getQuery().trim());
        }
        if (request.hasRole()) {
            typedQuery.setParameter("role", request.getRole());
        }
        if (request.hasCurrentCompany()) {
            typedQuery.setParameter("company", request.getCurrentCompany().trim());
        }
        if (request.hasSkills()) {
            List<String> skillsLower = request.getSkills().stream()
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .map(s -> s.trim().toLowerCase())
                    .toList();
            typedQuery.setParameter("skillsLower", skillsLower);
        }
        if (request.hasGraduationYear()) {
            typedQuery.setParameter("graduationYear", request.getGraduationYear());
        }

        return typedQuery.getResultList();
    }
}
