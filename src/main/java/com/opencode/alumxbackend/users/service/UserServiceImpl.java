package com.opencode.alumxbackend.users.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.opencode.alumxbackend.common.exception.Errors.BadRequestException;
import com.opencode.alumxbackend.users.dto.UserProfileResponse;
import com.opencode.alumxbackend.users.dto.UserProfileUpdateRequest;
import com.opencode.alumxbackend.users.dto.UserRequest;
import com.opencode.alumxbackend.users.dto.UserResponseDto;
import com.opencode.alumxbackend.users.model.User;
import com.opencode.alumxbackend.users.model.UserRole;
import com.opencode.alumxbackend.users.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User createUser(UserRequest request) {

        // 1️⃣ Check uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists: " + request.getEmail());
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists: " + request.getUsername());
        }

        // 2️⃣ Validate role
        UserRole role;
        try {
            role = UserRole.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role. Must be STUDENT, ALUMNI, or PROFESSOR.");
        }

        // 3️⃣ Optional: validate email format, password length etc.
        if (!request.getEmail().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}(\\.[A-Za-z]{2,})?$")) {
            throw new BadRequestException("Invalid email format: " + request.getEmail());
        }

        if (request.getPassword().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters");
        }

        // 4️⃣ Create and save user
        User user = User.builder()
                .username(request.getUsername())
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .profileCompleted(true) // default for dev
                .build();

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public UserProfileResponse getUserProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return mapToProfileDTO(user);
    }

    @Override
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    private UserResponseDto mapToResponseDTO(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
    private UserProfileResponse mapToProfileDTO(User user) {
        ProfileCompletion completion = calculateCompletion(user);

        return UserProfileResponse.builder()
                // Identity
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())

                // Professional summary
                .about(user.getAbout())
                .currentCompany(user.getCurrentCompany())
                .currentRole(user.getCurrentRole())
                .location(user.getLocation())

                // Links
                .linkedinUrl(user.getLinkedinUrl())
                .githubUrl(user.getGithubUrl())
                .portfolioUrl(user.getPortfolioUrl())

                // Skills & background
                .skills(copy(user.getSkills()))
                .education(copy(user.getEducation()))
                .techStack(copy(user.getTechStack()))
                .frameworks(copy(user.getFrameworks()))
                .languages(copy(user.getLanguages()))
                .communicationSkills(copy(user.getCommunicationSkills()))
                .softSkills(copy(user.getSoftSkills()))

                // Experience
                .experience(copy(user.getExperience()))
                .internships(copy(user.getInternships()))
                .projects(copy(user.getProjects()))
                .certifications(copy(user.getCertifications()))

                // Personal
                .hobbies(copy(user.getHobbies()))

                // Status
                .profileCompleted(completion.complete())
                .profileCompletionPercentage(completion.percentage())
                .missingFields(completion.missingFields())
                .build();
    }


    private List<String> copy(List<String> list) {
        return list == null ? List.of() : List.copyOf(list);
    }



    @Transactional
    public UserProfileResponse updateUserProfile(Long userId, UserProfileUpdateRequest request){

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Basic
        if (request.getName() != null)
            user.setName(request.getName());

        // Professional summary
        if (request.getAbout() != null)
            user.setAbout(request.getAbout());

        if (request.getCurrentCompany() != null)
            user.setCurrentCompany(request.getCurrentCompany());

        if (request.getCurrentRole() != null)
            user.setCurrentRole(request.getCurrentRole());

        if (request.getLocation() != null)
            user.setLocation(request.getLocation());

        // Links
        if (request.getLinkedinUrl() != null)
            user.setLinkedinUrl(request.getLinkedinUrl());

        if (request.getGithubUrl() != null)
            user.setGithubUrl(request.getGithubUrl());

        if (request.getPortfolioUrl() != null)
            user.setPortfolioUrl(request.getPortfolioUrl());

        // Skills & background
        if (request.getSkills() != null)
            user.setSkills(request.getSkills());

        if (request.getEducation() != null)
            user.setEducation(request.getEducation());

        if (request.getTechStack() != null)
            user.setTechStack(request.getTechStack());

        if (request.getFrameworks() != null)
            user.setFrameworks(request.getFrameworks());

        if (request.getLanguages() != null)
            user.setLanguages(request.getLanguages());

        if (request.getCommunicationSkills() != null)
            user.setCommunicationSkills(request.getCommunicationSkills());

        if (request.getSoftSkills() != null)
            user.setSoftSkills(request.getSoftSkills());

        // Experience
        if (request.getExperience() != null)
            user.setExperience(request.getExperience());

        if (request.getInternships() != null)
            user.setInternships(request.getInternships());

        if (request.getProjects() != null)
            user.setProjects(request.getProjects());

        if (request.getCertifications() != null)
            user.setCertifications(request.getCertifications());

        // Personal
        if (request.getHobbies() != null)
            user.setHobbies(request.getHobbies());

        user.setProfileCompleted(calculateCompletion(user).complete());

        User updatedUser = userRepository.save(user);
        return mapToProfileDTO(updatedUser);
    }

    /**
     * Computes how complete a user's profile is, based on a fixed set of key
     * fields ({@link ProfileField}). The percentage and missing-field list are
     * derived on every read/update and never persisted; only the boolean
     * {@code profileCompleted} flag is kept in sync on update so the stored
     * column stays meaningful for other consumers.
     */
    private ProfileCompletion calculateCompletion(User user) {
        List<String> missing = new ArrayList<>();
        for (ProfileField field : ProfileField.values()) {
            if (!field.isPopulated(user)) {
                missing.add(field.getKey());
            }
        }
        int total = ProfileField.values().length;
        int filled = total - missing.size();
        int percentage = (int) Math.round((double) filled / total * 100);
        return new ProfileCompletion(percentage, List.copyOf(missing));
    }

    /**
     * Outcome of a completeness check.
     *
     * @param percentage    completion as a whole-number percentage (0-100)
     * @param missingFields keys of the still-empty fields, matching the JSON
     *                      property names of the profile response / update request
     */
    record ProfileCompletion(int percentage, List<String> missingFields) {
        boolean complete() {
            return missingFields.isEmpty();
        }
    }

    /**
     * Key profile attributes that count towards completion. Each {@code key}
     * mirrors the JSON property name in {@code UserProfileResponse} /
     * {@code UserProfileUpdateRequest} so the frontend can map a missing entry
     * straight back to the input that still needs to be filled.
     */
    private enum ProfileField {
        ABOUT("about", u -> StringUtils.hasText(u.getAbout())),
        CURRENT_ROLE("currentRole", u -> StringUtils.hasText(u.getCurrentRole())),
        CURRENT_COMPANY("currentCompany", u -> StringUtils.hasText(u.getCurrentCompany())),
        LOCATION("location", u -> StringUtils.hasText(u.getLocation())),
        LINKEDIN_URL("linkedinUrl", u -> StringUtils.hasText(u.getLinkedinUrl())),
        GITHUB_URL("githubUrl", u -> StringUtils.hasText(u.getGithubUrl())),
        SKILLS("skills", u -> hasItems(u.getSkills())),
        EDUCATION("education", u -> hasItems(u.getEducation())),
        TECH_STACK("techStack", u -> hasItems(u.getTechStack())),
        FRAMEWORKS("frameworks", u -> hasItems(u.getFrameworks())),
        LANGUAGES("languages", u -> hasItems(u.getLanguages())),
        PROJECTS("projects", u -> hasItems(u.getProjects())),
        CERTIFICATIONS("certifications", u -> hasItems(u.getCertifications()));

        private final String key;
        private final Predicate<User> populated;

        ProfileField(String key, Predicate<User> populated) {
            this.key = key;
            this.populated = populated;
        }

        String getKey() {
            return key;
        }

        boolean isPopulated(User user) {
            return populated.test(user);
        }

        private static boolean hasItems(List<String> values) {
            return !CollectionUtils.isEmpty(values);
        }
    }
}
