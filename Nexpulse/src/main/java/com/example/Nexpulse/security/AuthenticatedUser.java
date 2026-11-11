package com.example.Nexpulse.security;

public record AuthenticatedUser(Long id, String email) {

	public static final String ROLE_USER = "ROLE_USER";

	@Override
	public String toString() {
		return "AuthenticatedUser[id=" + id + "]";
	}
}
