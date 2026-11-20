package com.freshmart.smartstock.security;

import com.freshmart.smartstock.domain.enums.Role;

/** Identity extracted from a validated JWT (claims: sub=userId, email, role). */
public record AuthUser(Long id, String email, Role role) {
}
