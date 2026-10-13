package com.xaxage.be.application.service;

import com.xaxage.be.application.port.in.ProvideDemoUserUseCase;
import com.xaxage.be.application.port.out.PasswordHasherPort;
import com.xaxage.be.application.port.out.UserRepositoryPort;
import com.xaxage.be.domain.model.DemoUser;
import com.xaxage.be.domain.model.User;
import org.springframework.stereotype.Service;

@Service
public class ProvideDemoUserService implements ProvideDemoUserUseCase {

    private final UserRepositoryPort users;
    private final PasswordHasherPort passwords;

    public ProvideDemoUserService(UserRepositoryPort users, PasswordHasherPort passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Override
    public void provide() {
        if (!users.existsByUsername(DemoUser.USERNAME)) {
            users.save(new User(DemoUser.USERNAME, passwords.hash(DemoUser.PASSWORD)));
        }
    }
}
