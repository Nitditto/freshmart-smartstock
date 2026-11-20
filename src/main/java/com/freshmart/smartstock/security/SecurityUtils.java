package com.freshmart.smartstock.security;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The ONLY way services learn who the caller is. UserId always comes from the validated token,
 * never from the request body/query (clients can lie).
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser user)) {
            throw ApiException.unauthorized("Chưa xác thực");
        }
        return user;
    }

    public static Long currentUserId() {
        return currentUser().id();
    }

    public static boolean isAdmin() {
        return currentUser().role() == Role.ADMIN;
    }
}
