package com.example.Nexpulse.dto;

import java.time.Instant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Public account profile returned by the API.
 */
public record UserResponse(
		@NotNull Long id,

		@NotBlank
		@Size(max = 120) String name,

		@NotBlank
		@Email
		@Size(max = 254) String email,

		@NotNull Instant createdAt
) {
}
