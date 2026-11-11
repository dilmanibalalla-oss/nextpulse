package com.example.Nexpulse.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Account details submitted to create a user. Carries the plaintext password only for this request.
 */
public record UserRegistrationRequest(
		@NotBlank
		@Size(max = 120) String name,

		@NotBlank
		@Email
		@Size(max = 254) String email,

		@NotBlank
		@Size(min = MIN_PASSWORD_LENGTH, max = MAX_PASSWORD_LENGTH) String password
) {

	public static final int MIN_PASSWORD_LENGTH = 8;

	public static final int MAX_PASSWORD_LENGTH = 72;

	@Override
	public String toString() {
		return "UserRegistrationRequest[name=" + name + ", email=" + email + "]";
	}
}
