package com.freshmart.smartstock.service;

import com.freshmart.smartstock.dto.auth.ChangePasswordRequest;
import com.freshmart.smartstock.dto.auth.LoginRequest;
import com.freshmart.smartstock.dto.auth.LoginResponse;
import com.freshmart.smartstock.dto.user.UserResponse;

public interface AuthService {

    /** Wrong email, wrong password and locked account all fail with the SAME 401 message. */
    LoginResponse login(LoginRequest request);

    /** The currently logged-in user (id comes from the token). */
    UserResponse me();

    /** Wrong current password or a new password equal to it -> 400. Stored as a fresh BCrypt hash. */
    void changePassword(ChangePasswordRequest request);
}
