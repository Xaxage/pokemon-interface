package com.xaxage.be.application.port.out;

import com.xaxage.be.domain.model.User;

public interface UserRepositoryPort {

    /**
     * @return whether someone is registered with the username
     */
    boolean existsByUsername(String username);

    /**
     * Stores the user.
     *
     * @return the user as stored
     */
    User save(User user);
}
