package com.example.Nexpulse.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DomainValidationTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void validEntitiesHaveNoViolations() {
		AppUser user = new AppUser("Ada Lovelace", "ada@example.com", "hash");
		Monitor monitor = new Monitor("Example", "https://example.com/health", 60, user);
		CheckResult result = new CheckResult(
				monitor,
				Instant.parse("2026-11-11T00:00:00Z"),
				true,
				200,
				35L,
				null
		);

		assertThat(validator.validate(user)).isEmpty();
		assertThat(validator.validate(monitor)).isEmpty();
		assertThat(validator.validate(result)).isEmpty();
	}

	@Test
	void rejectsBlankIdentityFieldsAndImpossibleMonitorValues() {
		AppUser user = new AppUser(" ", "not-an-email", " ");
		Monitor monitor = new Monitor(" ", "not a url", 0, new AppUser("Ada", "ada@example.com", "hash"));
		CheckResult result = new CheckResult();

		assertThat(paths(validator.validate(user))).contains("name", "email", "passwordHash");
		assertThat(paths(validator.validate(monitor))).contains("name", "url", "intervalSeconds");
		assertThat(paths(validator.validate(result))).contains("monitor", "checkedAt");
	}

	private static Set<String> paths(Set<? extends ConstraintViolation<?>> violations) {
		return violations.stream()
				.map(violation -> violation.getPropertyPath().toString())
				.collect(java.util.stream.Collectors.toSet());
	}
}
