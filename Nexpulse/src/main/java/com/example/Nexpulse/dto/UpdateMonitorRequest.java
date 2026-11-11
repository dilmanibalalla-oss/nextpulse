package com.example.Nexpulse.dto;

import com.example.Nexpulse.domain.Monitor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

/**
 * Replacement for the editable monitor settings. Status and check history stay server-owned.
 */
public record UpdateMonitorRequest(
		@NotBlank
		@Size(max = 120) String name,

		@NotBlank
		@Size(max = 2048)
		@URL String url,

		@NotNull
		@Min(Monitor.MIN_INTERVAL_SECONDS)
		@Max(CreateMonitorRequest.MAX_INTERVAL_SECONDS) Integer intervalSeconds,

		@NotNull Boolean enabled
) {
}
