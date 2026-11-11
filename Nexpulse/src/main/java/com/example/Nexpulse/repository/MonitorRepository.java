package com.example.Nexpulse.repository;

import java.util.List;
import java.util.Optional;

import com.example.Nexpulse.domain.Monitor;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitorRepository extends JpaRepository<Monitor, Long> {

	List<Monitor> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

	Optional<Monitor> findByIdAndOwnerId(Long id, Long ownerId);

	/**
	 * Every enabled monitor, for the scheduler that checks all of them.
	 * A request on behalf of one user uses {@link #findByOwnerIdAndEnabledTrue(Long)}.
	 */
	List<Monitor> findByEnabledTrue();

	List<Monitor> findByOwnerIdAndEnabledTrue(Long ownerId);
}
