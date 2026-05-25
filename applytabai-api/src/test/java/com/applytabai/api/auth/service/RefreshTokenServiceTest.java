package com.applytabai.api.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.applytabai.api.auth.config.SecurityProperties;
import com.applytabai.api.auth.domain.RefreshToken;
import com.applytabai.api.auth.repository.RefreshTokenRepository;
import com.applytabai.api.auth.service.impl.RefreshTokenServiceImpl;
import com.applytabai.api.common.exception.ApiException;
import com.applytabai.api.common.exception.ErrorCode;
import com.applytabai.api.users.domain.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

	private static final Instant NOW = Instant.parse("2026-05-24T12:00:00Z");

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	private RefreshTokenService refreshTokenService;

	@BeforeEach
	void setUp() {
		SecurityProperties properties = new SecurityProperties();
		properties.getJwt().setRefreshTokenTtl(Duration.ofDays(7));
		refreshTokenService = new RefreshTokenServiceImpl(
				refreshTokenRepository,
				properties,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	@Test
	void issueRefreshTokenStoresHashOnly() {
		when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

		RefreshTokenService.IssuedRefreshToken issued = refreshTokenService.issueRefreshToken(user());

		ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(refreshTokenCaptor.capture());

		assertThat(issued.value()).isNotBlank();
		assertThat(issued.tokenHash()).hasSize(64);
		assertThat(refreshTokenCaptor.getValue().getTokenHash()).isEqualTo(issued.tokenHash());
		assertThat(refreshTokenCaptor.getValue().getTokenHash()).isNotEqualTo(issued.value());
		assertThat(issued.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
	}

	@Test
	void requireActiveRejectsUnknownToken() {
		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> refreshTokenService.requireActive("missing-token"))
				.isInstanceOf(ApiException.class)
				.extracting("code")
				.isEqualTo(ErrorCode.TOKEN_INVALID);
	}

	@Test
	void requireActiveRejectsExpiredToken() {
		RefreshToken expired = RefreshToken.issue(user(), "a".repeat(64), NOW.minusSeconds(1));
		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

		assertThatThrownBy(() -> refreshTokenService.requireActive("expired-token"))
				.isInstanceOf(ApiException.class)
				.extracting("code")
				.isEqualTo(ErrorCode.TOKEN_INVALID);
	}

	@Test
	void rotateRevokesCurrentTokenAndIssuesReplacement() {
		when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
		RefreshToken current = RefreshToken.issue(user(), "a".repeat(64), NOW.plus(Duration.ofDays(7)));

		RefreshTokenService.IssuedRefreshToken replacement = refreshTokenService.rotate(current);

		assertThat(current.getRevokedAt()).isEqualTo(NOW);
		assertThat(current.getReplacedByTokenHash()).isEqualTo(replacement.tokenHash());
		assertThat(replacement.value()).isNotBlank();
		assertThat(replacement.tokenHash()).hasSize(64);
	}

	@Test
	void revokeMarksTokenAsRevokedWhenPresent() {
		RefreshToken refreshToken = RefreshToken.issue(user(), "a".repeat(64), NOW.plus(Duration.ofDays(7)));
		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));

		refreshTokenService.revoke("refresh-token");

		assertThat(refreshToken.getRevokedAt()).isEqualTo(NOW);
		verify(refreshTokenRepository).save(refreshToken);
	}

	private UserAccount user() {
		return UserAccount.registerLocal("jane@example.com", "Jane Developer", "encoded-password");
	}
}
