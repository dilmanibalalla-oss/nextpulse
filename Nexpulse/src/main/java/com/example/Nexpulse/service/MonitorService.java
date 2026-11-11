package com.example.Nexpulse.service;

import java.util.List;

import com.example.Nexpulse.domain.AppUser;
import com.example.Nexpulse.domain.CheckResult;
import com.example.Nexpulse.domain.Monitor;
import com.example.Nexpulse.dto.CheckResultResponse;
import com.example.Nexpulse.dto.CreateMonitorRequest;
import com.example.Nexpulse.dto.MonitorResponse;
import com.example.Nexpulse.dto.UpdateMonitorRequest;
import com.example.Nexpulse.repository.AppUserRepository;
import com.example.Nexpulse.repository.CheckResultRepository;
import com.example.Nexpulse.repository.MonitorRepository;
import com.example.Nexpulse.security.AuthenticatedUser;
import com.example.Nexpulse.security.InvalidTokenException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonitorService {

	private final MonitorRepository monitors;

	private final CheckResultRepository checkResults;

	private final AppUserRepository users;

	public MonitorService(
			MonitorRepository monitors,
			CheckResultRepository checkResults,
			AppUserRepository users
	) {
		this.monitors = monitors;
		this.checkResults = checkResults;
		this.users = users;
	}

	@Transactional
	public MonitorResponse create(CreateMonitorRequest request) {
		AuthenticatedUser current = currentUser();
		AppUser owner = users.findById(current.id()).orElseThrow(InvalidTokenException::new);
		Monitor monitor = new Monitor(
				request.name().trim(),
				request.url().trim(),
				request.intervalSeconds(),
				owner
		);
		monitor.setEnabled(request.enabled());
		monitors.saveAndFlush(monitor);
		return toResponse(monitor);
	}

	@Transactional(readOnly = true)
	public List<MonitorResponse> list() {
		return monitors.findByOwnerIdOrderByCreatedAtDesc(currentUser().id()).stream()
				.map(MonitorService::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public MonitorResponse get(Long id) {
		return toResponse(ownedMonitor(id));
	}

	@Transactional
	public MonitorResponse update(Long id, UpdateMonitorRequest request) {
		Monitor monitor = ownedMonitor(id);
		monitor.setName(request.name().trim());
		monitor.setUrl(request.url().trim());
		monitor.setIntervalSeconds(request.intervalSeconds());
		monitor.setEnabled(request.enabled());
		return toResponse(monitor);
	}

	@Transactional
	public void delete(Long id) {
		Long ownerId = currentUser().id();
		if (monitors.findByIdAndOwnerId(id, ownerId).isEmpty()) {
			throw new MonitorNotFoundException();
		}
		checkResults.deleteByMonitorIdAndMonitorOwnerId(id, ownerId);
		monitors.delete(monitors.findByIdAndOwnerId(id, ownerId).orElseThrow(MonitorNotFoundException::new));
	}

	@Transactional(readOnly = true)
	public Page<CheckResultResponse> history(Long id, int page, int size) {
		Long ownerId = currentUser().id();
		ownedMonitor(id);
		return checkResults
				.findByMonitorIdAndMonitorOwnerIdOrderByCheckedAtDesc(id, ownerId, PageRequest.of(page, size))
				.map(result -> toResponse(result, id));
	}

	private Monitor ownedMonitor(Long id) {
		return monitors.findByIdAndOwnerId(id, currentUser().id()).orElseThrow(MonitorNotFoundException::new);
	}

	private static AuthenticatedUser currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
			throw new InvalidCredentialsException();
		}
		return user;
	}

	private static MonitorResponse toResponse(Monitor monitor) {
		return new MonitorResponse(
				monitor.getId(),
				monitor.getName(),
				monitor.getUrl(),
				monitor.getIntervalSeconds(),
				monitor.getStatus(),
				monitor.getLastStatusCode(),
				monitor.getLastResponseTime(),
				monitor.getLastCheckedAt(),
				monitor.isEnabled(),
				monitor.getCreatedAt(),
				monitor.getOwner().getId()
		);
	}

	private static CheckResultResponse toResponse(CheckResult result, Long monitorId) {
		return new CheckResultResponse(
				result.getId(),
				monitorId,
				result.getCheckedAt(),
				result.isSuccessful(),
				result.getStatusCode(),
				result.getResponseTimeMs(),
				result.getErrorMessage()
		);
	}
}
