package com.freshmart.smartstock.controller;

import com.freshmart.smartstock.common.api.ApiResponse;
import com.freshmart.smartstock.dto.auth.ChangePasswordRequest;
import com.freshmart.smartstock.dto.auth.LoginRequest;
import com.freshmart.smartstock.dto.auth.LoginResponse;
import com.freshmart.smartstock.dto.user.UserResponse;
import com.freshmart.smartstock.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Login and current user")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements // public endpoint: no lock icon in Swagger
    @Operation(summary = "Login and receive a JWT access token (60 minutes)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
                    @ExampleObject(name = "ADMIN", value = "{\"email\":\"admin@freshmart.local\","
                            + "\"password\":\"FreshMart@2026\"}")})))
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the currently logged-in user")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.ok(authService.me());
    }

    @PatchMapping("/password")
    @Operation(summary = "Change the current user's password (needs the current password)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
                    @ExampleObject(name = "Change password", value = "{\"currentPassword\":\"FreshMart@2026\","
                            + "\"newPassword\":\"NewPass@2026\"}")})))
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.ok(null);
    }
}
