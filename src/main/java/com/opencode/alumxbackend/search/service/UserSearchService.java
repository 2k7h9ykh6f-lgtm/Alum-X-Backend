package com.opencode.alumxbackend.search.service;

import com.opencode.alumxbackend.users.dto.UserResponseDto;
import java.util.List;

public interface UserSearchService {

    List<UserResponseDto> search(String query);

    List<UserResponseDto> search(String query,
                                 String role,
                                 String currentCompany,
                                 List<String> skills,
                                 Integer graduationYear);
}
