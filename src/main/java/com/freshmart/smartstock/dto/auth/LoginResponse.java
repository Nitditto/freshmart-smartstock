package com.freshmart.smartstock.dto.auth;

import com.freshmart.smartstock.dto.user.UserResponse;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {
}
