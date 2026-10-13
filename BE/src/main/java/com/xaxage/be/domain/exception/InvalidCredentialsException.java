package com.xaxage.be.domain.exception;

/**
 * The username and password given do not belong together; which of the two is wrong is never said.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("The username or password is wrong");
    }
}
