package com.xaxage.be.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Who wants to sign in; the values are only checked against the registered user, never against the
 * registration limits.
 */
public record SignInRequest(@NotNull String username, @NotNull String password) {
}
