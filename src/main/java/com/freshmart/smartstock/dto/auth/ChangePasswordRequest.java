package com.freshmart.smartstock.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 72 is the BCrypt input limit: longer passwords would be silently truncated. */
public record ChangePasswordRequest(
        @NotBlank(message = "Mật khẩu hiện tại không được để trống") String currentPassword,
        @NotBlank @Size(min = 8, max = 72, message = "Mật khẩu mới phải từ 8 đến 72 ký tự") String newPassword) {
}
