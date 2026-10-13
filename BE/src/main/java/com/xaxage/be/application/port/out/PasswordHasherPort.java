package com.xaxage.be.application.port.out;

public interface PasswordHasherPort {

    /**
     * @return a one-way hash of the password, safe to store
     */
    String hash(String password);
}
