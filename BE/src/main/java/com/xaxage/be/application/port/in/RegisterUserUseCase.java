package com.xaxage.be.application.port.in;

import com.xaxage.be.domain.model.User;

public interface RegisterUserUseCase {

    /**
     * @return the user registered with the username and password
     */
    User register(String username, String password);
}
