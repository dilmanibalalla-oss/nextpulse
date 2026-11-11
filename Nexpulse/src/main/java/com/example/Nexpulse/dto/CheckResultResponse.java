package com.example.Nexpulse.dto;

import java.time.Instant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * One stored check, identified by monitor id rather than a monitor entity.
 */
public record CheckResultResponse(
		@NotNull Long id,

		@NotNull Long monitorId,

		@NotNull Instant checkedAt,

		boolean successful,

		@Min(100)
		@Max(599) Integer statusCode,

		/** Time until the response completed, in milliseconds. Null when none was measured. */
		@PositiveOrZero Long responseTimeMs,

		@Size(max = 1000) String errorMessage
) {
}
