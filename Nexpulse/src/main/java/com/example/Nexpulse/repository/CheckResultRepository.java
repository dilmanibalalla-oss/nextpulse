package com.example.Nexpulse.repository;

import com.example.Nexpulse.domain.CheckResult;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckResultRepository extends JpaRepository<CheckResult, Long> {

	Page<CheckResult> findByMonitorIdAndMonitorOwnerIdOrderByCheckedAtDesc(
			Long monitorId,
			Long ownerId,
			Pageable pageable
	);
}
