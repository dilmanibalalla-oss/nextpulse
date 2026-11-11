package com.example.Nexpulse.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


@Entity
@Table(
		name = "app_users",
		uniqueConstraints = @UniqueConstraint(name = "uk_app_users_email", columnNames = "email")
)
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 120)
	@Column(nullable = false, length = 120)
	private String name;

	@NotBlank
	@Email
	@Size(max = 254)
	@Column(nullable = false, length = 254)
	private String email;

	@NotBlank
	@Size(max = 255)
	@JsonIgnore
	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@JsonIgnore
	@OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Monitor> monitors = new ArrayList<>();

	protected AppUser() {
	}

	public AppUser(String name, String email, String passwordHash) {
		this.name = name;
		this.email = email;
		this.passwordHash = passwordHash;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public void addMonitor(Monitor monitor) {
		if (monitor == null) {
			throw new IllegalArgumentException("monitor is required");
		}
		if (!monitors.contains(monitor)) {
			monitors.add(monitor);
		}
		monitor.assignOwner(this);
	}

	public void removeMonitor(Monitor monitor) {
		monitors.remove(monitor);
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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@JsonIgnore
	public String getPasswordHash() {
		return passwordHash;
	}

	@JsonIgnore
	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	@JsonIgnore
	public List<Monitor> getMonitors() {
		return List.copyOf(monitors);
	}

	@Override
	public final boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AppUser user)) {
			return false;
		}
		return id != null && id.equals(user.getId());
	}

	@Override
	public final int hashCode() {
		return AppUser.class.hashCode();
	}
}
