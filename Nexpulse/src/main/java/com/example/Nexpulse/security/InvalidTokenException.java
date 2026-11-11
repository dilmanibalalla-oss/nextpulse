package com.example.Nexpulse.security;

import org.springframework.security.core.AuthenticationException;

public class InvalidTokenException extends AuthenticationException {

	public InvalidTokenException() {
		super("Invalid token");
	}
}
