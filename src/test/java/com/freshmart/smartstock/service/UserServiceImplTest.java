package com.freshmart.smartstock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.entity.User;
import com.freshmart.smartstock.domain.enums.Role;
import com.freshmart.smartstock.dto.user.CreateUserRequest;
import com.freshmart.smartstock.dto.user.UpdateUserStatusRequest;
import com.freshmart.smartstock.dto.user.UserResponse;
import com.freshmart.smartstock.repository.UserRepository;
import com.freshmart.smartstock.security.AuthUser;
import com.freshmart.smartstock.service.impl.UserServiceImpl;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final String RAW_PASSWORD = "Secret@123";

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userRepository, passwordEncoder);
        AuthUser admin = new AuthUser(ADMIN_ID, "admin@test.local", Role.ADMIN);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(admin, null, List.of()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRejectsDuplicateEmailIgnoringCaseWith409() {
        when(userRepository.existsByEmailIgnoreCase("a@x.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.create(new CreateUserRequest("A", "  A@X.com ", RAW_PASSWORD, Role.STAFF)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createStoresNormalizedEmailAndBcryptHashNotRawPassword() {
        when(userRepository.existsByEmailIgnoreCase("a@x.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = service.create(new CreateUserRequest(" An ", "  A@X.com ", RAW_PASSWORD, Role.STAFF));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("a@x.com", saved.getValue().getEmail());
        assertEquals("An", saved.getValue().getFullName());
        assertTrue(saved.getValue().getPasswordHash().startsWith("$2"));
        assertTrue(passwordEncoder.matches(RAW_PASSWORD, saved.getValue().getPasswordHash()));
        assertTrue(response.active());
        assertEquals(Role.STAFF, response.role());
    }

    @Test
    void findAllMapsUsersToResponses() {
        when(userRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(user(2L, true)));

        assertEquals(1, service.findAll().size());
    }

    @Test
    void updateStatusRejectsLockingOwnAccountWith409() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(user(ADMIN_ID, true)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateStatus(ADMIN_ID, new UpdateUserStatusRequest(false)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void updateStatusAllowsUnlockingOwnAccount() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(user(ADMIN_ID, false)));

        assertTrue(service.updateStatus(ADMIN_ID, new UpdateUserStatusRequest(true)).active());
    }

    @Test
    void updateStatusReturns404WhenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateStatus(99L, new UpdateUserStatusRequest(false)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void updateStatusLocksAnotherUser() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, true)));

        assertFalse(service.updateStatus(2L, new UpdateUserStatusRequest(false)).active());
    }

    @Test
    void updateStatusIsIdempotentForAlreadyLockedUser() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, false)));

        assertFalse(service.updateStatus(2L, new UpdateUserStatusRequest(false)).active());
    }

    @Test
    void userResponseHasNoPasswordHashComponent() {
        boolean hasHash = Arrays.stream(UserResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .anyMatch(name -> name.toLowerCase().contains("password"));

        assertFalse(hasHash);
    }

    private User user(Long id, boolean active) {
        User u = new User();
        u.setId(id);
        u.setFullName("User " + id);
        u.setEmail("u" + id + "@x.com");
        u.setPasswordHash("$2a$hash");
        u.setRole(Role.STAFF);
        u.setActive(active);
        return u;
    }
}
