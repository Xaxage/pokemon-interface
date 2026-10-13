package com.xaxage.be.application.service;

import com.xaxage.be.application.port.in.RegisterUserUseCase;
import com.xaxage.be.application.port.out.PasswordHasherPort;
import com.xaxage.be.application.port.out.UserRepositoryPort;
import com.xaxage.be.domain.exception.UsernameTakenException;
import com.xaxage.be.domain.model.User;
import org.springframework.stereotype.Service;

@Service
public class RegisterUserService implements RegisterUserUseCase {

    private final UserRepositoryPort users;
    private final PasswordHasherPort passwords;

    public RegisterUserService(UserRepositoryPort users, PasswordHasherPort passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Override
    public User register(String username, String password) {
        if (users.existsByUsername(username)) {
            throw new UsernameTakenException(username);
        }
        return users.save(new User(username, passwords.hash(password)));
    }
}
