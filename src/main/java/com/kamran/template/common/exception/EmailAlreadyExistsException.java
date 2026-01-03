package com.kamran.template.common.exception;

/**
 * Thrown when attempting to create/update user with email that already exists.
 * Results in 409 Conflict response.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email already in use: "+ email);
    }
}
