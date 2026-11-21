package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.entity.User;
import com.freshmart.smartstock.dto.user.CreateUserRequest;
import com.freshmart.smartstock.dto.user.UpdateUserStatusRequest;
import com.freshmart.smartstock.dto.user.UserResponse;
import com.freshmart.smartstock.repository.UserRepository;
import com.freshmart.smartstock.security.SecurityUtils;
import com.freshmart.smartstock.service.UserService;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll(Sort.by("id")).stream().map(UserResponse::from).toList();
    }

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Email đã được sử dụng");
        }
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setRole(request.role());
        user.setActive(true);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updateStatus(Long id, UpdateUserStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản " + id));
        boolean isLockingSelf = !request.active() && user.getId().equals(SecurityUtils.currentUserId());
        if (isLockingSelf) {
            throw ApiException.conflict("Không thể tự khóa tài khoản của chính mình");
        }
        // Idempotent: locking an already locked account still returns 200.
        user.setActive(request.active());
        return UserResponse.from(user);
    }
}
