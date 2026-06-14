package com.opencode.alumxbackend.search.service;

import com.opencode.alumxbackend.search.dto.UserSearchRequest;
import com.opencode.alumxbackend.users.dto.UserResponseDto;
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
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }
        return repository.searchUsers(query.trim());
    }

    @Override
    public List<UserResponseDto> search(UserSearchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Search request cannot be null");
        }
        // When only query is provided (no filters), behave like the original search
        if (!request.hasRole() && !request.hasCurrentCompany()
                && !request.hasSkills() && !request.hasGraduationYear()) {
            if (!request.hasQuery()) {
                throw new IllegalArgumentException("Search query cannot be empty");
            }
            return repository.searchUsers(request.getQuery().trim());
        }
        // Trim query if present
        if (request.hasQuery()) {
            request.setQuery(request.getQuery().trim());
        }
        return repository.searchUsers(request);
    }
}
