package com.xaxage.be.application.port.in;

import com.xaxage.be.domain.model.AccessToken;

public interface SignInUseCase {

    /**
     * @return a token showing the user is signed in, valid for {@link AccessToken#LIFETIME}
     */
    AccessToken signIn(String username, String password);
}
