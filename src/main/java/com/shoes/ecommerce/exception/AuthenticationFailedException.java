package com.shoes.ecommerce.exception;

public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException(String message) { super(message); }
}
