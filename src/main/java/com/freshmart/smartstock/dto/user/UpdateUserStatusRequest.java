package com.freshmart.smartstock.dto.user;

import jakarta.validation.constraints.NotNull;

/** active=false locks the account (login then fails with the same generic 401 message). */
public record UpdateUserStatusRequest(@NotNull Boolean active) {
}
