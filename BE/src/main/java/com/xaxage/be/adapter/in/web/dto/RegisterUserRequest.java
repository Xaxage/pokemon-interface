package com.xaxage.be.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Who wants to register: a username of 3 to 30 characters and a password of at least 8.
 */
public record RegisterUserRequest(@NotNull @Size(min = 3, max = 30) String username,
                                  @NotNull @Size(min = 8) String password) {
}
