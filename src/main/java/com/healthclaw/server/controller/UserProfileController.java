package com.healthclaw.server.controller;

import com.healthclaw.server.dto.ApiResponse;
import com.healthclaw.server.entity.UserProfile;
import com.healthclaw.server.service.UserProfileService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    private final UserProfileService service;

    public UserProfileController(UserProfileService service) {
        this.service = service;
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfile> getProfile() {
        return ApiResponse.ok(service.getProfile());
    }

    @PutMapping("/profile")
    public ApiResponse<UserProfile> updateProfile(@RequestBody UserProfile profile) {
        return ApiResponse.ok(service.saveProfile(profile));
    }
}
