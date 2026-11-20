package com.freshmart.smartstock.dto.user;

import com.freshmart.smartstock.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự") String password,
        @NotNull Role role) {
}
