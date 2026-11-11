package com.example.Nexpulse.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import com.example.Nexpulse.domain.AppUser;
import com.example.Nexpulse.domain.CheckResult;
import com.example.Nexpulse.domain.Monitor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryQueryTest {

	@Autowired
	private AppUserRepository users;

	@Autowired
	private MonitorRepository monitors;

	@Autowired
	private CheckResultRepository checkResults;

	@Test
	void findsAUserByEmail() {
		String email = emailFor("ada");
		users.saveAndFlush(new AppUser("Ada Lovelace", email, "hash-ada"));

		assertThat(users.findByEmail(email))
				.get()
				.extracting(AppUser::getName, AppUser::getEmail)
				.containsExactly("Ada Lovelace", email);
		assertThat(users.findByEmail(emailFor("missing"))).isEmpty();
	}

	@Test
	void monitorQueriesStayInsideTheOwnersAccount() {
		AppUser ada = users.saveAndFlush(ownerWithMonitors("Ada"));
		AppUser grace = users.saveAndFlush(ownerWithMonitors("Grace"));

		Monitor adaApi = monitorNamed(ada, "API");
		Monitor adaPaused = monitorNamed(ada, "Paused");

		assertThat(monitors.findByOwnerIdOrderByCreatedAtDesc(ada.getId()))
				.extracting(Monitor::getName)
				.containsExactlyInAnyOrder("API", "Paused");
		assertThat(monitors.findByOwnerIdOrderByCreatedAtDesc(grace.getId()))
				.extracting(Monitor::getId)
				.doesNotContain(adaApi.getId(), adaPaused.getId());

		assertThat(monitors.findByIdAndOwnerId(adaApi.getId(), ada.getId())).contains(adaApi);
		assertThat(monitors.findByIdAndOwnerId(adaApi.getId(), grace.getId())).isEmpty();

		assertThat(monitors.findByOwnerIdAndEnabledTrue(ada.getId()))
				.extracting(Monitor::getName)
				.containsExactly("API");
		assertThat(monitors.findByEnabledTrue())
				.extracting(Monitor::getId)
				.contains(adaApi.getId(), monitorNamed(grace, "API").getId())
				.doesNotContain(adaPaused.getId(), monitorNamed(grace, "Paused").getId());
	}

	@Test
	void checkHistoryIsPagedAndHiddenFromOtherUsers() {
		AppUser ada = new AppUser("Ada", emailFor("ada"), "hash-ada");
		Monitor api = new Monitor("API", "https://example.com/api", 30, ada);
		Instant oldest = Instant.parse("2026-11-11T12:00:00Z");
		Instant middle = Instant.parse("2026-11-11T12:01:00Z");
		Instant newest = Instant.parse("2026-11-11T12:02:00Z");
		new CheckResult(api, oldest, true, 200, 20L, null);
		new CheckResult(api, middle, true, 204, 25L, null);
		new CheckResult(api, newest, false, 503, 1500L, "unavailable");
		users.saveAndFlush(ada);

		AppUser grace = users.saveAndFlush(new AppUser("Grace", emailFor("grace"), "hash-grace"));

		Page<CheckResult> firstPage = checkResults.findByMonitorIdAndMonitorOwnerIdOrderByCheckedAtDesc(
				api.getId(),
				ada.getId(),
				PageRequest.of(0, 2)
		);
		Page<CheckResult> secondPage = checkResults.findByMonitorIdAndMonitorOwnerIdOrderByCheckedAtDesc(
				api.getId(),
				ada.getId(),
				PageRequest.of(1, 2)
		);
		Page<CheckResult> hidden = checkResults.findByMonitorIdAndMonitorOwnerIdOrderByCheckedAtDesc(
				api.getId(),
				grace.getId(),
				PageRequest.of(0, 2)
		);

		assertThat(firstPage.getTotalElements()).isEqualTo(3);
		assertThat(firstPage.hasNext()).isTrue();
		assertThat(firstPage.getContent()).extracting(CheckResult::getCheckedAt).containsExactly(newest, middle);
		assertThat(secondPage.getContent()).extracting(CheckResult::getCheckedAt).containsExactly(oldest);
		assertThat(hidden.getContent()).isEmpty();
		assertThat(hidden.getTotalElements()).isZero();
	}

	private static AppUser ownerWithMonitors(String name) {
		AppUser owner = new AppUser(name, emailFor(name), "hash-" + name);
		new Monitor("API", "https://example.com/" + name + "/api", 30, owner);
		Monitor paused = new Monitor("Paused", "https://example.com/" + name + "/paused", 120, owner);
		paused.setEnabled(false);
		return owner;
	}

	private static Monitor monitorNamed(AppUser owner, String name) {
		return owner.getMonitors().stream()
				.filter(monitor -> monitor.getName().equals(name))
				.findFirst()
				.orElseThrow();
	}

	private static String emailFor(String name) {
		return name.toLowerCase() + "-" + UUID.randomUUID() + "@nexpulse.test";
	}
}
