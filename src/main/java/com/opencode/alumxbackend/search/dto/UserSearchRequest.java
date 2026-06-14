package com.opencode.alumxbackend.search.dto;

import com.opencode.alumxbackend.users.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchRequest {
    private String query;
    private UserRole role;
    private String currentCompany;
    private List<String> skills;
    private Integer graduationYear;

    public boolean hasRole() {
        return role != null;
    }

    public boolean hasCurrentCompany() {
        return currentCompany != null && !currentCompany.trim().isEmpty();
    }

    public boolean hasSkills() {
        return skills != null && !skills.isEmpty();
    }

    public boolean hasGraduationYear() {
        return graduationYear != null;
    }

    public boolean hasQuery() {
        return query != null && !query.trim().isEmpty();
    }
}
