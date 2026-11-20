package com.freshmart.smartstock.dto.user;

import com.freshmart.smartstock.domain.entity.User;
import com.freshmart.smartstock.domain.enums.Role;
import java.time.Instant;

/** Deliberately has NO passwordHash field - it must never leave the server. */
public record UserResponse(Long id, String fullName, String email, Role role, boolean active, Instant createdAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.isActive(), u.getCreatedAt());
    }
}
