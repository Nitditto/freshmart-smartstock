package com.freshmart.smartstock.service;

import com.freshmart.smartstock.dto.user.CreateUserRequest;
import com.freshmart.smartstock.dto.user.UpdateUserStatusRequest;
import com.freshmart.smartstock.dto.user.UserResponse;
import java.util.List;

public interface UserService {

    List<UserResponse> findAll();

    /** Email must be unique (409). Password stored as BCrypt hash. */
    UserResponse create(CreateUserRequest request);

    /** Lock / unlock. An admin must not be able to lock their own account (409). */
    UserResponse updateStatus(Long id, UpdateUserStatusRequest request);
}
