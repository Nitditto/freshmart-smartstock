package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.dto.user.CreateUserRequest;
import com.freshmart.smartstock.dto.user.UpdateUserStatusRequest;
import com.freshmart.smartstock.dto.user.UserResponse;
import com.freshmart.smartstock.service.UserService;
import java.util.List;
import org.springframework.stereotype.Service;

/** TODO[Member D]: implement. Use UserRepository + PasswordEncoder; never return the hash. */
@Service
public class UserServiceImpl implements UserService {

    @Override
    public List<UserResponse> findAll() {
        throw ApiException.notImplemented("Member D");
    }

    @Override
    public UserResponse create(CreateUserRequest request) {
        throw ApiException.notImplemented("Member D");
    }

    @Override
    public UserResponse updateStatus(Long id, UpdateUserStatusRequest request) {
        throw ApiException.notImplemented("Member D");
    }
}
