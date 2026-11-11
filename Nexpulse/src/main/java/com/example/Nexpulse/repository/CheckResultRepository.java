package com.example.Nexpulse.repository;

import com.example.Nexpulse.domain.CheckResult;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CheckResultRepository extends JpaRepository<CheckResult, Long> {

	Page<CheckResult> findByMonitorIdAndMonitorOwnerIdOrderByCheckedAtDesc(
			Long monitorId,
			Long ownerId,
			Pageable pageable
	);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			delete from CheckResult result
			where result.monitor.id = :monitorId
			and result.monitor.owner.id = :ownerId
			""")
	void deleteByMonitorIdAndMonitorOwnerId(@Param("monitorId") Long monitorId, @Param("ownerId") Long ownerId);
}
