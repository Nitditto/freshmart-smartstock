package com.freshmart.smartstock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.entity.User;
import com.freshmart.smartstock.domain.enums.Role;
import com.freshmart.smartstock.dto.auth.ChangePasswordRequest;
import com.freshmart.smartstock.repository.UserRepository;
import com.freshmart.smartstock.security.AuthUser;
import com.freshmart.smartstock.security.JwtService;
import com.freshmart.smartstock.service.impl.AuthServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final Long USER_ID = 7L;
    private static final String OLD_PASSWORD = "OldPass@123";
    private static final String NEW_PASSWORD = "NewPass@456";

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private AuthServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(userRepository, passwordEncoder, jwtService);
        user = new User();
        user.setId(USER_ID);
        user.setPasswordHash(passwordEncoder.encode(OLD_PASSWORD));
        AuthUser me = new AuthUser(USER_ID, "u@x.com", Role.STAFF);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(me, null, List.of()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void changePasswordStoresNewBcryptHash() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        service.changePassword(new ChangePasswordRequest(OLD_PASSWORD, NEW_PASSWORD));

        assertTrue(user.getPasswordHash().startsWith("$2"));
        assertTrue(passwordEncoder.matches(NEW_PASSWORD, user.getPasswordHash()));
    }

    @Test
    void changePasswordRejectsWrongCurrentPasswordWith400() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.changePassword(new ChangePasswordRequest("wrong", NEW_PASSWORD)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(passwordEncoder.matches(OLD_PASSWORD, user.getPasswordHash()));
    }

    @Test
    void changePasswordRejectsSamePasswordWith400() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.changePassword(new ChangePasswordRequest(OLD_PASSWORD, OLD_PASSWORD)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void changePasswordReturns401WhenAccountNoLongerExists() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.changePassword(new ChangePasswordRequest(OLD_PASSWORD, NEW_PASSWORD)));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }
}
