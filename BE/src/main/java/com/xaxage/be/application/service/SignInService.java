package com.xaxage.be.application.service;

import com.xaxage.be.application.port.in.SignInUseCase;
import com.xaxage.be.application.port.out.PasswordHasherPort;
import com.xaxage.be.application.port.out.TokenIssuerPort;
import com.xaxage.be.application.port.out.UserRepositoryPort;
import com.xaxage.be.domain.exception.InvalidCredentialsException;
import com.xaxage.be.domain.model.AccessToken;
import com.xaxage.be.domain.model.User;
import org.springframework.stereotype.Service;

@Service
public class SignInService implements SignInUseCase {

    private final UserRepositoryPort users;
    private final PasswordHasherPort passwords;
    private final TokenIssuerPort tokens;

    /**
     * What the password of someone not registered is checked against, so that signing in as them takes as long as
     * signing in with a wrong password.
     */
    private final String hashOfNobody;

    public SignInService(UserRepositoryPort users, PasswordHasherPort passwords, TokenIssuerPort tokens) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
        this.hashOfNobody = passwords.hash("password-of-nobody");
    }

    @Override
    public AccessToken signIn(String username, String password) {
        var user = users.findByUsername(username);
        var passwordMatches = passwords.matches(password, user.map(User::passwordHash).orElse(hashOfNobody));
        if (user.isEmpty() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }
        return new AccessToken(tokens.issue(username, AccessToken.LIFETIME));
    }
}
