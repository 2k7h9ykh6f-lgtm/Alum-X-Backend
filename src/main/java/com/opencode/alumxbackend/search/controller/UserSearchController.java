package com.opencode.alumxbackend.search.controller;

import com.opencode.alumxbackend.users.dto.UserResponseDto;
import com.opencode.alumxbackend.search.service.UserSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserSearchController {

    private final UserSearchService service;

    @GetMapping("/search")
    public ResponseEntity<List<UserResponseDto>> searchUsers(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "currentCompany", required = false) String currentCompany,
            @RequestParam(value = "skills", required = false) List<String> skills,
            @RequestParam(value = "graduationYear", required = false) Integer graduationYear) {
        return ResponseEntity.ok(service.search(query, role, currentCompany, skills, graduationYear));
    }
}
