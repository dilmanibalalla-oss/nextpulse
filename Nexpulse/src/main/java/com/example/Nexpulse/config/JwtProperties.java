package com.example.Nexpulse.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nexpulse.jwt")
public record JwtProperties(String secret, Duration expiration) {

	public static final int MIN_SECRET_BYTES = 32;

	public static final Duration MIN_EXPIRATION = Duration.ofMinutes(1);

	public static final Duration MAX_EXPIRATION = Duration.ofHours(24);

	public JwtProperties {
		if (secret == null || secret.isBlank()) {
			throw new IllegalArgumentException("JWT_SECRET is required");
		}
		if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
			throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes");
		}
		if (expiration == null
				|| expiration.compareTo(MIN_EXPIRATION) < 0
				|| expiration.compareTo(MAX_EXPIRATION) > 0) {
			throw new IllegalArgumentException("JWT_EXPIRATION must be between 1 minute and 24 hours");
		}
	}
}
