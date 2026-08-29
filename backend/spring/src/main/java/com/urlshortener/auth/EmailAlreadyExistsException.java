package com.urlshortener.auth;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException() { super("An account with that email already exists"); }
}
