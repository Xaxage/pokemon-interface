package com.xaxage.be.domain.model;

import java.time.Duration;

/**
 * Shows that a user has signed in; it unlocks the protected actions until it expires.
 *
 * @param value the signed token, as the reader sends it back
 */
public record AccessToken(String value) {

    /**
     * How long a token stays valid after it is issued.
     */
    public static final Duration LIFETIME = Duration.ofHours(1);
}
