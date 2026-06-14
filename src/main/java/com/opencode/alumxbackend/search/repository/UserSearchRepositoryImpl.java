package com.opencode.alumxbackend.search.repository;

import com.opencode.alumxbackend.users.dto.UserResponseDto;
import com.opencode.alumxbackend.users.model.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserSearchRepositoryImpl implements UserSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<UserResponseDto> searchUsers(String query,
                                             UserRole role,
                                             String currentCompany,
                                             List<String> skills,
                                             Integer graduationYear) {

        boolean hasQuery = query != null && !query.isBlank();
        boolean hasCompany = currentCompany != null && !currentCompany.isBlank();
        List<String> skillFilters = (skills == null) ? List.of() : skills.stream()
                .filter(s -> s != null && !s.isBlank())
                .toList();

        StringBuilder jpql = new StringBuilder("""
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
                WHERE 1 = 1
                """);

        // Free-text keyword search keeps the original OR-across-fields behaviour.
        if (hasQuery) {
            jpql.append("""
                    AND (
                        LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(u.name) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(u.currentCompany) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(e) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(exp) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(i) LIKE LOWER(CONCAT('%', :q, '%'))
                    )
                    """);
        }

        // Each optional filter narrows the result set (AND-combined).
        if (role != null) {
            jpql.append(" AND u.role = :role ");
        }

        if (hasCompany) {
            jpql.append(" AND LOWER(u.currentCompany) LIKE LOWER(CONCAT('%', :company, '%')) ");
        }

        // A user must possess every requested skill (case-insensitive contains).
        for (int idx = 0; idx < skillFilters.size(); idx++) {
            jpql.append(" AND EXISTS (SELECT sk FROM User su").append(idx)
                    .append(" JOIN su").append(idx).append(".skills sk WHERE su").append(idx)
                    .append(" = u AND LOWER(sk) LIKE LOWER(CONCAT('%', :skill").append(idx)
                    .append(", '%'))) ");
        }

        // No dedicated graduationYear column exists, so match it against the free-text education entries.
        if (graduationYear != null) {
            jpql.append(" AND EXISTS (SELECT ed FROM User gu JOIN gu.education ed "
                    + "WHERE gu = u AND LOWER(ed) LIKE LOWER(CONCAT('%', :graduationYear, '%'))) ");
        }

        TypedQuery<UserResponseDto> typedQuery =
                entityManager.createQuery(jpql.toString(), UserResponseDto.class);

        if (hasQuery) {
            typedQuery.setParameter("q", query.trim());
        }
        if (role != null) {
            typedQuery.setParameter("role", role);
        }
        if (hasCompany) {
            typedQuery.setParameter("company", currentCompany.trim());
        }
        for (int idx = 0; idx < skillFilters.size(); idx++) {
            typedQuery.setParameter("skill" + idx, skillFilters.get(idx).trim());
        }
        if (graduationYear != null) {
            typedQuery.setParameter("graduationYear", String.valueOf(graduationYear));
        }

        return typedQuery.getResultList();
    }
}
