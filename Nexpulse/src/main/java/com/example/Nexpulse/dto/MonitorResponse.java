package com.example.Nexpulse.dto;

import java.time.Instant;

import com.example.Nexpulse.domain.Monitor;
import com.example.Nexpulse.domain.MonitorStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

/**
 * Monitor state returned to its owner. Check history is served by {@link CheckResultResponse}.
 */
public record MonitorResponse(
		@NotNull Long id,

		@NotBlank
		@Size(max = 120) String name,

		@NotBlank
		@Size(max = 2048)
		@URL String url,

		@NotNull
		@Min(Monitor.MIN_INTERVAL_SECONDS)
		@Max(CreateMonitorRequest.MAX_INTERVAL_SECONDS) Integer intervalSeconds,

		@NotNull MonitorStatus status,

		@Min(100)
		@Max(599) Integer lastStatusCode,

		/** Latest measured response time, in milliseconds. Null until a check records one. */
		@PositiveOrZero Long lastResponseTime,

		Instant lastCheckedAt,

		boolean enabled,

		@NotNull Instant createdAt,

		@NotNull Long ownerId
) {
}
