package com.opencode.alumxbackend.search.repository;

import com.opencode.alumxbackend.users.dto.UserResponseDto;
import com.opencode.alumxbackend.users.model.UserRole;

import java.util.List;

public interface UserSearchRepository {
    List<UserResponseDto> searchUsers(String query,
                                      UserRole role,
                                      String currentCompany,
                                      List<String> skills,
                                      Integer graduationYear);
}
