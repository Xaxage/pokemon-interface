package com.xaxage.be.application.port.out;

import com.xaxage.be.domain.model.User;

import java.util.Optional;

public interface UserRepositoryPort {

    /**
     * @return whether someone is registered with the username
     */
    boolean existsByUsername(String username);

    /**
     * @return the user registered with the username, or empty when no one is
     */
    Optional<User> findByUsername(String username);

    /**
     * Stores the user.
     *
     * @return the user as stored
     */
    User save(User user);
}
