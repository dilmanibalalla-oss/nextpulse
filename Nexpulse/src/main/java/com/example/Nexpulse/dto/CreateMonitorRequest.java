package com.example.Nexpulse.dto;

import com.example.Nexpulse.domain.Monitor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

/**
 * Settings for a new monitor. The owner comes from the authenticated user, not from this body.
 */
public record CreateMonitorRequest(
		@NotBlank
		@Size(max = 120) String name,

		@NotBlank
		@Size(max = 2048)
		@URL String url,

		@NotNull
		@Min(Monitor.MIN_INTERVAL_SECONDS)
		@Max(MAX_INTERVAL_SECONDS) Integer intervalSeconds,

		@NotNull Boolean enabled
) {

	public static final int MAX_INTERVAL_SECONDS = 86_400;
}
