package com.xaxage.be.application.port.out;

import java.time.Duration;

public interface TokenIssuerPort {

    /**
     * @return a signed token showing the user is signed in, valid from now for the lifetime given
     */
    String issue(String username, Duration lifetime);
}
