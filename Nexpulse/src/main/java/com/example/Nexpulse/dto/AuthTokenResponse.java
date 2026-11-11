package com.example.Nexpulse.dto;

import java.time.Instant;

public record AuthTokenResponse(String token, Instant expiresAt, UserResponse user) {

	@Override
	public String toString() {
		Long userId = user == null ? null : user.id();
		return "AuthTokenResponse[expiresAt=" + expiresAt + ", userId=" + userId + "]";
	}
}
