package com.example.Nexpulse.repository;

import java.util.Optional;

import com.example.Nexpulse.domain.AppUser;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	Optional<AppUser> findByEmail(String email);
}
