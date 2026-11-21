package com.freshmart.smartstock.controller;

import com.freshmart.smartstock.common.api.ApiResponse;
import com.freshmart.smartstock.dto.user.CreateUserRequest;
import com.freshmart.smartstock.dto.user.UpdateUserStatusRequest;
import com.freshmart.smartstock.dto.user.UserResponse;
import com.freshmart.smartstock.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Users", description = "Account management (ADMIN only)")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List all user accounts")
    public ApiResponse<List<UserResponse>> findAll() {
        return ApiResponse.ok(userService.findAll());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a user account",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
                    @ExampleObject(name = "STAFF", value = "{\"fullName\":\"Nguyễn Văn A\","
                            + "\"email\":\"staff2@freshmart.local\",\"password\":\"Staff@2026\","
                            + "\"role\":\"STAFF\"}")})))
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.created(userService.create(request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Lock / unlock a user account",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
                    @ExampleObject(name = "Lock", value = "{\"active\":false}")})))
    public ApiResponse<UserResponse> updateStatus(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateUserStatusRequest request) {
        return ApiResponse.ok(userService.updateStatus(id, request));
    }
}
