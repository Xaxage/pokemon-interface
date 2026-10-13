package com.xaxage.be.domain.model;

/**
 * Someone registered to sign in and make changes.
 *
 * @param passwordHash a one-way hash of the user's password; the password itself is never kept
 */
public record User(String username, String passwordHash) {
}
