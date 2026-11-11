package com.example.Nexpulse.domain;

import java.time.Instant;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;


@Entity
@Table(
		name = "check_results",
		indexes = @Index(name = "idx_check_results_monitor_checked_at", columnList = "monitor_id, checked_at"),
		check = {
				@CheckConstraint(
						name = "chk_check_results_status_code",
						constraint = "status_code is null or (status_code >= 100 and status_code <= 599)"
				),
				@CheckConstraint(
						name = "chk_check_results_response_time",
						constraint = "response_time_ms is null or response_time_ms >= 0"
				)
		}
)
public class CheckResult {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "monitor_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_check_results_monitor")
	)
	private Monitor monitor;

	@NotNull
	@Column(nullable = false, updatable = false)
	private Instant checkedAt;

	@Column(nullable = false)
	private boolean successful;

	@Min(100)
	@Max(599)
	@Column
	private Integer statusCode;

	/** Time until the response completed, in milliseconds. Null when no response was measured. */
	@PositiveOrZero
	@Column(name = "response_time_ms")
	private Long responseTimeMs;

	@Size(max = 1000)
	@Column(length = 1000)
	private String errorMessage;

	protected CheckResult() {
	}

	public CheckResult(
			Monitor monitor,
			Instant checkedAt,
			boolean successful,
			Integer statusCode,
			Long responseTimeMs,
			String errorMessage
	) {
		if (checkedAt == null) {
			throw new IllegalArgumentException("checkedAt is required");
		}
		this.checkedAt = checkedAt;
		this.successful = successful;
		this.statusCode = statusCode;
		this.responseTimeMs = responseTimeMs;
		this.errorMessage = errorMessage;
		if (monitor == null) {
			throw new IllegalArgumentException("monitor is required");
		}
		monitor.addCheckResult(this);
	}

	void assignMonitor(Monitor monitor) {
		this.monitor = monitor;
	}

	public Long getId() {
		return id;
	}

	public Monitor getMonitor() {
		return monitor;
	}

	public Instant getCheckedAt() {
		return checkedAt;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public Integer getStatusCode() {
		return statusCode;
	}

	public Long getResponseTimeMs() {
		return responseTimeMs;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public final boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof CheckResult result)) {
			return false;
		}
		return id != null && id.equals(result.getId());
	}

	@Override
	public final int hashCode() {
		return CheckResult.class.hashCode();
	}
}
