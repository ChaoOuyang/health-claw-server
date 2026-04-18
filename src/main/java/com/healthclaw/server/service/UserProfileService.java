package com.healthclaw.server.service;

import com.healthclaw.server.entity.UserProfile;
import com.healthclaw.server.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

    private final UserProfileRepository repo;

    public UserProfileService(UserProfileRepository repo) {
        this.repo = repo;
    }

    public UserProfile getProfile() {
        return repo.findById(1).orElseGet(() -> {
            UserProfile p = new UserProfile();
            return repo.save(p);
        });
    }

    public UserProfile saveProfile(UserProfile profile) {
        profile.setId(1);
        profile.setUpdatedAt(java.time.LocalDateTime.now());
        return repo.save(profile);
    }
}
