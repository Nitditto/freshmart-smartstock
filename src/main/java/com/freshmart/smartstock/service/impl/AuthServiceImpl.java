package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.entity.User;
import com.freshmart.smartstock.dto.auth.LoginRequest;
import com.freshmart.smartstock.dto.auth.LoginResponse;
import com.freshmart.smartstock.dto.user.UserResponse;
import com.freshmart.smartstock.repository.UserRepository;
import com.freshmart.smartstock.security.AuthUser;
import com.freshmart.smartstock.security.JwtService;
import com.freshmart.smartstock.security.SecurityUtils;
import com.freshmart.smartstock.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reference implementation: finished in the skeleton so every member can log in on day 1. */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** One message for every failure so attackers cannot tell which emails exist or are locked. */
    private static final String BAD_CREDENTIALS = "Email hoặc mật khẩu không đúng";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> ApiException.unauthorized(BAD_CREDENTIALS));
        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized(BAD_CREDENTIALS);
        }
        String token = jwtService.generateToken(new AuthUser(user.getId(), user.getEmail(), user.getRole()));
        return new LoginResponse(token, "Bearer", jwtService.getExpiresInSeconds(), UserResponse.from(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse me() {
        return userRepository.findById(SecurityUtils.currentUserId())
                .map(UserResponse::from)
                .orElseThrow(() -> ApiException.unauthorized("Tài khoản không còn tồn tại"));
    }
}
