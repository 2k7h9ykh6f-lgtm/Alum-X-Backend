package com.opencode.alumxbackend.search.service;

import com.opencode.alumxbackend.users.dto.UserResponseDto;
import com.opencode.alumxbackend.users.model.UserRole;
import com.opencode.alumxbackend.search.repository.UserSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSearchServiceImpl implements UserSearchService {

    private final UserSearchRepository repository;

    @Override
    public List<UserResponseDto> search(String query) {
        return search(query, null, null, null, null);
    }

    @Override
    public List<UserResponseDto> search(String query,
                                        String role,
                                        String currentCompany,
                                        List<String> skills,
                                        Integer graduationYear) {

        String normalizedQuery = (query == null || query.trim().isEmpty()) ? null : query.trim();

        UserRole normalizedRole = null;
        if (role != null && !role.trim().isEmpty()) {
            try {
                normalizedRole = UserRole.valueOf(role.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid role: " + role.trim());
            }
        }

        String normalizedCompany = (currentCompany == null || currentCompany.trim().isEmpty())
                ? null : currentCompany.trim();

        List<String> normalizedSkills = (skills == null) ? List.of() : skills.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(String::trim)
                .toList();

        boolean hasCriteria = normalizedQuery != null
                || normalizedRole != null
                || normalizedCompany != null
                || !normalizedSkills.isEmpty()
                || graduationYear != null;

        if (!hasCriteria) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        return repository.searchUsers(normalizedQuery, normalizedRole, normalizedCompany,
                normalizedSkills, graduationYear);
    }
}
