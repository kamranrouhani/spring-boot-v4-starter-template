package com.kamran.template.common.exception;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("Please verify your email before logging in. Check your inbox for verification link.");
    }
}
