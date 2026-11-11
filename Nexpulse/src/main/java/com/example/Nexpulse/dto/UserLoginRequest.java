package com.example.Nexpulse.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Email and password submitted to authenticate an existing user.
 */
public record UserLoginRequest(
		@NotBlank
		@Email
		@Size(max = 254) String email,

		@NotBlank
		@Size(min = UserRegistrationRequest.MIN_PASSWORD_LENGTH, max = UserRegistrationRequest.MAX_PASSWORD_LENGTH)
		String password
) {
}
