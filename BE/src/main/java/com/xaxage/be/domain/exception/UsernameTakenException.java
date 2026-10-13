package com.xaxage.be.domain.exception;

/**
 * Someone is already registered with the username chosen.
 */
public class UsernameTakenException extends RuntimeException {

    public UsernameTakenException(String username) {
        super("The username " + username + " is already taken");
    }
}
