package com.xaxage.be.adapter.in.web.dto;

import com.xaxage.be.domain.model.AccessToken;

/**
 * @param token the bearer token to send back on protected actions
 */
public record AccessTokenResponse(String token) {

    public static AccessTokenResponse from(AccessToken token) {
        return new AccessTokenResponse(token.value());
    }
}
