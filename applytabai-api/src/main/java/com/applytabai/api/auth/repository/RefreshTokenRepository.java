package com.applytabai.api.auth.repository;

import java.util.Optional;
import java.util.UUID;

import com.applytabai.api.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);
}
