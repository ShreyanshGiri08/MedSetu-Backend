package com.medsetu.service;

import com.medsetu.dto.ApiResponse;
import com.medsetu.entity.User;
import com.medsetu.entity.UserProfile;
import com.medsetu.exception.ResourceNotFoundException;
import com.medsetu.repository.UserProfileRepository;
import com.medsetu.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public UserService(UserRepository userRepository, UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public Map<String, Object> getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("name", user.getName());
        result.put("email", user.getEmail());
        result.put("phone", user.getPhone());
        result.put("role", user.getRole());
        result.put("isActive", user.getIsActive());
        result.put("createdAt", user.getCreatedAt());

        if (profile != null) {
            result.put("gender", profile.getGender());
            result.put("dateOfBirth", profile.getDateOfBirth());
            result.put("address", profile.getAddress());
            result.put("profilePicUrl", profile.getProfilePicUrl());
        }
        return result;
    }

    @Transactional
    public Map<String, Object> updateProfile(Long userId, Map<String, String> updates) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (updates.containsKey("name")) user.setName(updates.get("name"));
        if (updates.containsKey("phone")) user.setPhone(updates.get("phone"));
        userRepository.save(user);

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElse(UserProfile.builder().user(user).build());

        if (updates.containsKey("gender") && updates.get("gender") != null) {
            try {
                profile.setGender(UserProfile.Gender.valueOf(updates.get("gender").toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (updates.containsKey("dateOfBirth") && updates.get("dateOfBirth") != null) {
            profile.setDateOfBirth(LocalDate.parse(updates.get("dateOfBirth")));
        }
        if (updates.containsKey("address")) profile.setAddress(updates.get("address"));
        if (updates.containsKey("profilePicUrl")) profile.setProfilePicUrl(updates.get("profilePicUrl"));

        userProfileRepository.save(profile);

        return getProfile(userId);
    }
}
