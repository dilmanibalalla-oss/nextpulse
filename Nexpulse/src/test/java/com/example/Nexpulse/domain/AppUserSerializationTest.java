package com.example.Nexpulse.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import tools.jackson.databind.json.JsonMapper;

@JsonTest
class AppUserSerializationTest {

	private static final String PASSWORD_HASH = "stored-password-hash";

	@Autowired
	private JsonMapper jsonMapper;

	@Test
	void userJsonOmitsPasswordHash() {
		AppUser user = new AppUser("Ada Lovelace", "ada@example.com", PASSWORD_HASH);

		String json = jsonMapper.writeValueAsString(user);

		assertThat(json).contains("Ada Lovelace", "ada@example.com");
		assertThat(json).doesNotContain(PASSWORD_HASH, "passwordHash", "password_hash");
	}

	@Test
	void nestedUserInsideMonitorJsonOmitsPasswordHash() {
		AppUser user = new AppUser("Ada Lovelace", "ada@example.com", PASSWORD_HASH);
		Monitor monitor = new Monitor("Example", "https://example.com", 60, user);
		monitor.recordCheck(MonitorStatus.UP, 200, 40L, Instant.parse("2026-11-11T00:00:00Z"));
		new CheckResult(
				monitor,
				Instant.parse("2026-11-11T00:00:00Z"),
				false,
				503,
				40L,
				"hidden-check-error"
		);

		String monitorJson = jsonMapper.writeValueAsString(monitor);
		String resultJson = jsonMapper.writeValueAsString(monitor.getCheckResults().get(0));

		assertThat(monitorJson).contains("Example", "https://example.com", "Ada Lovelace");
		assertThat(monitorJson).doesNotContain(PASSWORD_HASH, "passwordHash", "hidden-check-error");
		assertThat(resultJson).contains("hidden-check-error", "https://example.com");
		assertThat(resultJson).doesNotContain(PASSWORD_HASH, "passwordHash");
	}
}
