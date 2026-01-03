package com.kamran.template.common.exception;

/**
 * Thrown when a user is not found by ID or email.
 * Results in 404 Not Found response.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
       super("User not found with id: " + id);
   }

   public UserNotFoundException(String email) {
       super("User not found with email: "+ email);
   }
}
