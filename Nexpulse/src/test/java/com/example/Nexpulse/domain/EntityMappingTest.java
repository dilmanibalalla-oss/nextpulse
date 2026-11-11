package com.example.Nexpulse.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EntityMappingTest {

	@Autowired
	private EntityManager entityManager;

	@Test
	void persistsOwnerMonitorsAndCheckHistory() {
		String email = "ada-" + UUID.randomUUID() + "@nexpulse.test";
		String passwordHash = "stored-password-hash";
		Instant checkedAt = Instant.parse("2026-11-11T12:00:00Z");

		AppUser user = new AppUser("Ada Lovelace", email, passwordHash);
		Monitor unchecked = new Monitor("Docs", "https://example.com/docs", 60, user);
		Monitor checked = new Monitor("API", "https://example.com/api", 30, user);
		checked.recordCheck(MonitorStatus.DOWN, 503, 1500L, checkedAt);
		new CheckResult(checked, checkedAt, false, 503, 1500L, "unavailable");
		Instant earlier = checkedAt.minusSeconds(30);
		new CheckResult(checked, earlier, true, 200, 80L, null);

		entityManager.persist(user);
		entityManager.flush();
		entityManager.clear();

		AppUser storedUser = entityManager.find(AppUser.class, user.getId());
		assertThat(storedUser.getCreatedAt()).isNotNull();
		assertThat(storedUser.getEmail()).isEqualTo(email);
		assertThat(storedUser.getPasswordHash()).isEqualTo(passwordHash);
		assertThat(storedUser.getMonitors()).hasSize(2);

		Monitor storedChecked = storedUser.getMonitors().stream()
				.filter(monitor -> monitor.getName().equals("API"))
				.findFirst()
				.orElseThrow();
		Monitor storedUnchecked = storedUser.getMonitors().stream()
				.filter(monitor -> monitor.getName().equals("Docs"))
				.findFirst()
				.orElseThrow();

		assertThat(storedUnchecked.getStatus()).isEqualTo(MonitorStatus.UNKNOWN);
		assertThat(storedUnchecked.getLastCheckedAt()).isNull();
		assertThat(storedUnchecked.isEnabled()).isTrue();
		assertThat(storedUnchecked.getCreatedAt()).isNotNull();
		assertThat(storedUnchecked.getOwner().getId()).isEqualTo(storedUser.getId());

		assertThat(storedChecked.getStatus()).isEqualTo(MonitorStatus.DOWN);
		assertThat(storedChecked.getLastStatusCode()).isEqualTo(503);
		assertThat(storedChecked.getLastResponseTime()).isEqualTo(1500L);
		assertThat(storedChecked.getLastCheckedAt()).isEqualTo(checkedAt);
		assertThat(storedChecked.getCheckResults()).extracting(CheckResult::getCheckedAt)
				.containsExactly(checkedAt, earlier);
		assertThat(storedChecked.getCheckResults().get(0).getErrorMessage()).isEqualTo("unavailable");
		assertThat(storedChecked.getCheckResults().get(0).isSuccessful()).isFalse();
		assertThat(storedChecked.getCheckResults().get(1).getStatusCode()).isEqualTo(200);

		String status = (String) entityManager.createNativeQuery("select status from monitors where id = :id")
				.setParameter("id", storedUnchecked.getId())
				.getSingleResult();
		String storedHash = (String) entityManager.createNativeQuery(
				"select password_hash from app_users where id = :id")
				.setParameter("id", storedUser.getId())
				.getSingleResult();
		Number ownerId = (Number) entityManager.createNativeQuery("select owner_id from monitors where id = :id")
				.setParameter("id", storedChecked.getId())
				.getSingleResult();

		assertThat(status).isEqualTo("UNKNOWN");
		assertThat(storedHash).isEqualTo(passwordHash);
		assertThat(ownerId.longValue()).isEqualTo(storedUser.getId());
	}

	@Test
	void rejectsADuplicateEmail() {
		String email = "duplicate-" + UUID.randomUUID() + "@nexpulse.test";
		entityManager.persist(new AppUser("Ada", email, "hash-one"));
		entityManager.flush();

		assertThatThrownBy(() -> {
			entityManager.persist(new AppUser("Grace", email, "hash-two"));
			entityManager.flush();
		}).isInstanceOf(PersistenceException.class);
	}

	@Test
	void removingAMonitorDeletesItsCheckResults() {
		AppUser user = new AppUser("Ada", "remove-" + UUID.randomUUID() + "@nexpulse.test", "hash");
		Monitor monitor = new Monitor("API", "https://example.com/api", 30, user);
		new CheckResult(monitor, Instant.parse("2026-11-11T12:00:00Z"), true, 200, 20L, null);
		entityManager.persist(user);
		entityManager.flush();

		Long monitorId = monitor.getId();
		AppUser storedUser = entityManager.find(AppUser.class, user.getId());
		Monitor storedMonitor = storedUser.getMonitors().get(0);
		storedUser.removeMonitor(storedMonitor);
		entityManager.flush();
		entityManager.clear();

		assertThat(entityManager.find(Monitor.class, monitorId)).isNull();
		Number remaining = (Number) entityManager.createNativeQuery(
				"select count(*) from check_results where monitor_id = :id")
				.setParameter("id", monitorId)
				.getSingleResult();
		assertThat(remaining.longValue()).isZero();
	}
}
