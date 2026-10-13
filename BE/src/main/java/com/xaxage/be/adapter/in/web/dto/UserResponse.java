package com.xaxage.be.adapter.in.web.dto;

import com.xaxage.be.domain.model.User;

/**
 * A registered user as the reader sees it: never with the password.
 */
public record UserResponse(String username) {

    public static UserResponse from(User user) {
        return new UserResponse(user.username());
    }
}
