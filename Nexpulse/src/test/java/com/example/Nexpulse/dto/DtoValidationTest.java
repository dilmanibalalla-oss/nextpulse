package com.example.Nexpulse.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;

import com.example.Nexpulse.domain.AppUser;
import com.example.Nexpulse.domain.CheckResult;
import com.example.Nexpulse.domain.Monitor;
import com.example.Nexpulse.domain.MonitorStatus;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DtoValidationTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void acceptsCompleteRequestsAndResponses() {
		assertThat(validator.validate(registration())).isEmpty();
		assertThat(validator.validate(new UserLoginRequest("ada@example.com", "a-valid-password"))).isEmpty();
		assertThat(validator.validate(userResponse())).isEmpty();
		assertThat(validator.validate(createMonitor(true))).isEmpty();
		assertThat(validator.validate(createMonitor(false))).isEmpty();
		assertThat(validator.validate(updateMonitor(Monitor.MIN_INTERVAL_SECONDS))).isEmpty();
		assertThat(validator.validate(updateMonitor(CreateMonitorRequest.MAX_INTERVAL_SECONDS))).isEmpty();
		assertThat(validator.validate(monitorResponse())).isEmpty();
		assertThat(validator.validate(new CheckResultResponse(
				9L,
				4L,
				Instant.parse("2026-11-11T12:00:00Z"),
				false,
				null,
				null,
				null
		))).isEmpty();
		assertThat(validator.validate(new DashboardStatsResponse(0, 0, 0, 0, 0))).isEmpty();
	}

	@Test
	void rejectsRegistrationWithBlankNameInvalidEmailAndShortPassword() {
		UserRegistrationRequest request = new UserRegistrationRequest(" ", "not-an-email", "short");

		assertThat(paths(validator.validate(request))).containsExactlyInAnyOrder("name", "email", "password");
	}

	@Test
	void rejectsRegistrationWhenEveryFieldIsMissing() {
		UserRegistrationRequest request = new UserRegistrationRequest(null, null, null);

		assertThat(paths(validator.validate(request))).containsExactlyInAnyOrder("name", "email", "password");
	}

	@Test
	void rejectsLoginWithAnInvalidEmailAndBlankPassword() {
		UserLoginRequest request = new UserLoginRequest("ada@", " ");

		assertThat(paths(validator.validate(request))).containsExactlyInAnyOrder("email", "password");
	}

	@Test
	void rejectsMonitorWritesWithABadUrlAndInterval() {
		CreateMonitorRequest created = new CreateMonitorRequest(" ", "not a url", 0, null);
		UpdateMonitorRequest updated = new UpdateMonitorRequest(
				"API",
				"https://example.com/health",
				CreateMonitorRequest.MAX_INTERVAL_SECONDS + 1,
				true
		);

		assertThat(paths(validator.validate(created)))
				.containsExactlyInAnyOrder("name", "url", "intervalSeconds", "enabled");
		assertThat(paths(validator.validate(updated))).containsExactly("intervalSeconds");
	}

	@Test
	void rejectsImpossibleCheckAndDashboardValues() {
		CheckResultResponse result = new CheckResultResponse(
				null,
				null,
				null,
				false,
				99,
				-1L,
				"x".repeat(1001)
		);
		MonitorResponse monitor = new MonitorResponse(
				4L,
				"API",
				"https://example.com/health",
				30,
				null,
				600,
				-5L,
				null,
				true,
				Instant.parse("2026-11-11T12:00:00Z"),
				7L
		);

		assertThat(paths(validator.validate(result)))
				.containsExactlyInAnyOrder("id", "monitorId", "checkedAt", "statusCode", "responseTimeMs", "errorMessage");
		assertThat(paths(validator.validate(monitor)))
				.containsExactlyInAnyOrder("status", "lastStatusCode", "lastResponseTime");
		assertThat(paths(validator.validate(new DashboardStatsResponse(-1, 0, 0, 0, 0))))
				.containsExactly("totalMonitors");
	}

	@Test
	void passwordHashesAndEntitiesStayOutOfTheApiTypes() {
		assertThat(componentNames(UserRegistrationRequest.class)).contains("password").doesNotContain("passwordHash");
		assertThat(componentNames(UserLoginRequest.class)).contains("password").doesNotContain("passwordHash");
		assertThat(componentNames(UserResponse.class)).containsExactly("id", "name", "email", "createdAt");
		assertThat(componentNames(MonitorResponse.class)).doesNotContain("owner", "checkResults", "passwordHash");
		assertThat(componentNames(CheckResultResponse.class)).contains("monitorId").doesNotContain("monitor");
		assertThat(componentTypes(MonitorResponse.class)).doesNotContain(AppUser.class, Monitor.class, CheckResult.class);
		assertThat(componentTypes(CheckResultResponse.class)).doesNotContain(Monitor.class, AppUser.class);
		assertThat(componentTypes(UserResponse.class)).doesNotContain(AppUser.class);
		assertThat(componentTypes(DashboardStatsResponse.class))
				.doesNotContain(AppUser.class, Monitor.class, CheckResult.class);
	}

	private static UserRegistrationRequest registration() {
		return new UserRegistrationRequest("Ada Lovelace", "ada@example.com", "a-valid-password");
	}

	private static UserResponse userResponse() {
		return new UserResponse(3L, "Ada Lovelace", "ada@example.com", Instant.parse("2026-11-11T12:00:00Z"));
	}

	private static CreateMonitorRequest createMonitor(boolean enabled) {
		return new CreateMonitorRequest("API", "https://example.com/health", 60, enabled);
	}

	private static UpdateMonitorRequest updateMonitor(int intervalSeconds) {
		return new UpdateMonitorRequest("API", "https://example.com/health", intervalSeconds, true);
	}

	private static MonitorResponse monitorResponse() {
		return new MonitorResponse(
				4L,
				"API",
				"https://example.com/health",
				60,
				MonitorStatus.UNKNOWN,
				null,
				null,
				null,
				true,
				Instant.parse("2026-11-11T12:00:00Z"),
				3L
		);
	}

	private static Set<String> paths(Set<? extends ConstraintViolation<?>> violations) {
		return violations.stream()
				.map(violation -> violation.getPropertyPath().toString())
				.collect(java.util.stream.Collectors.toSet());
	}

	private static Set<String> componentNames(Class<?> type) {
		return Arrays.stream(type.getRecordComponents())
				.map(RecordComponent::getName)
				.collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
	}

	private static Set<Class<?>> componentTypes(Class<?> type) {
		return Arrays.stream(type.getRecordComponents())
				.map(RecordComponent::getType)
				.collect(java.util.stream.Collectors.toSet());
	}
}
