package com.example.Nexpulse.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;


@Entity
@Table(
		name = "monitors",
		indexes = @Index(name = "idx_monitors_owner_id", columnList = "owner_id"),
		check = {
				@CheckConstraint(name = "chk_monitors_interval_seconds", constraint = "interval_seconds >= 1"),
				@CheckConstraint(
						name = "chk_monitors_status",
						constraint = "status in ('UNKNOWN', 'UP', 'DOWN')"
				),
				@CheckConstraint(
						name = "chk_monitors_last_status_code",
						constraint = "last_status_code is null or (last_status_code >= 100 and last_status_code <= 599)"
				),
				@CheckConstraint(
						name = "chk_monitors_last_response_time",
						constraint = "last_response_time is null or last_response_time >= 0"
				)
		}
)
public class Monitor {

	public static final int MIN_INTERVAL_SECONDS = 1;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 120)
	@Column(nullable = false, length = 120)
	private String name;

	@NotBlank
	@Size(max = 2048)
	@URL
	@Column(nullable = false, length = 2048)
	private String url;

	@Min(MIN_INTERVAL_SECONDS)
	@Column(nullable = false)
	private int intervalSeconds;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private MonitorStatus status = MonitorStatus.UNKNOWN;

	@Min(100)
	@Max(599)
	@Column
	private Integer lastStatusCode;

	/** Latest measured response time, in milliseconds. Null until a check records one. */
	@PositiveOrZero
	@Column
	private Long lastResponseTime;

	@Column
	private Instant lastCheckedAt;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "owner_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_monitors_owner")
	)
	private AppUser owner;

	@JsonIgnore
	@OneToMany(mappedBy = "monitor", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("checkedAt DESC")
	private List<CheckResult> checkResults = new ArrayList<>();

	protected Monitor() {
	}

	public Monitor(String name, String url, int intervalSeconds, AppUser owner) {
		this.name = name;
		this.url = url;
		this.intervalSeconds = intervalSeconds;
		if (owner == null) {
			throw new IllegalArgumentException("owner is required");
		}
		owner.addMonitor(this);
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	/**
	 * Stores the outcome of the most recent check on this monitor.
	 * Response time is milliseconds. Status code and response time stay null when the check
	 * produced none, for example when the connection failed.
	 */
	public void recordCheck(MonitorStatus status, Integer statusCode, Long responseTime, Instant checkedAt) {
		if (status == null) {
			throw new IllegalArgumentException("status is required");
		}
		if (checkedAt == null) {
			throw new IllegalArgumentException("checkedAt is required");
		}
		this.status = status;
		this.lastStatusCode = statusCode;
		this.lastResponseTime = responseTime;
		this.lastCheckedAt = checkedAt;
	}

	public void addCheckResult(CheckResult result) {
		if (result == null) {
			throw new IllegalArgumentException("check result is required");
		}
		if (!checkResults.contains(result)) {
			checkResults.add(result);
		}
		result.assignMonitor(this);
	}

	public void removeCheckResult(CheckResult result) {
		checkResults.remove(result);
	}

	void assignOwner(AppUser owner) {
		this.owner = owner;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getIntervalSeconds() {
		return intervalSeconds;
	}

	public void setIntervalSeconds(int intervalSeconds) {
		this.intervalSeconds = intervalSeconds;
	}

	public MonitorStatus getStatus() {
		return status;
	}

	public Integer getLastStatusCode() {
		return lastStatusCode;
	}

	public Long getLastResponseTime() {
		return lastResponseTime;
	}

	public Instant getLastCheckedAt() {
		return lastCheckedAt;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public AppUser getOwner() {
		return owner;
	}

	@JsonIgnore
	public List<CheckResult> getCheckResults() {
		return List.copyOf(checkResults);
	}

	@Override
	public final boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof Monitor monitor)) {
			return false;
		}
		return id != null && id.equals(monitor.getId());
	}

	@Override
	public final int hashCode() {
		return Monitor.class.hashCode();
	}
}
