package com.applytabai.api.auth.service;

import java.time.Instant;
import java.util.Optional;

import com.applytabai.api.auth.domain.RefreshToken;
import com.applytabai.api.auth.dto.AuthTokenResponse;
import com.applytabai.api.auth.dto.LoginRequest;
import com.applytabai.api.auth.dto.LogoutRequest;
import com.applytabai.api.auth.dto.RefreshTokenRequest;
import com.applytabai.api.auth.dto.RegisterRequest;
import com.applytabai.api.auth.service.impl.AuthServiceImpl;
import com.applytabai.api.common.exception.ApiException;
import com.applytabai.api.common.exception.ErrorCode;
import com.applytabai.api.users.domain.UserAccount;
import com.applytabai.api.users.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserAccountRepository userAccountRepository;

	@Mock
	private JwtTokenService jwtTokenService;

	@Mock
	private RefreshTokenService refreshTokenService;

	private PasswordEncoder passwordEncoder;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		passwordEncoder = new BCryptPasswordEncoder();
		authService = new AuthServiceImpl(userAccountRepository, passwordEncoder, jwtTokenService, refreshTokenService);
	}

	@Test
	void registerCreatesLocalUserAndReturnsTokenPair() {
		when(userAccountRepository.existsByEmail("jane@example.com")).thenReturn(false);
		when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
		stubIssuedTokens();

		AuthTokenResponse response = authService.register(new RegisterRequest(
				"  JANE@Example.COM ",
				"Jane Developer",
				"password123",
				"password123"
		));

		ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
		verify(userAccountRepository).save(userCaptor.capture());
		UserAccount savedUser = userCaptor.getValue();

		assertThat(savedUser.getEmail()).isEqualTo("jane@example.com");
		assertThat(savedUser.hasLocalPassword()).isTrue();
		assertThat(passwordEncoder.matches("password123", savedUser.getPasswordHash())).isTrue();
		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.refreshToken()).isEqualTo("refresh-token");
	}

	@Test
	void registerRejectsDuplicateEmail() {
		when(userAccountRepository.existsByEmail("jane@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(new RegisterRequest(
				"jane@example.com",
				"Jane Developer",
				"password123",
				"password123"
		)))
				.isInstanceOf(ApiException.class)
				.extracting("code")
				.isEqualTo(ErrorCode.CONFLICT);

		verify(userAccountRepository, never()).save(any());
	}

	@Test
	void registerRejectsMismatchedPasswordConfirmation() {
		assertThatThrownBy(() -> authService.register(new RegisterRequest(
				"jane@example.com",
				"Jane Developer",
				"password123",
				"different-password"
		)))
				.isInstanceOf(ApiException.class)
				.extracting("code")
				.isEqualTo(ErrorCode.VALIDATION_FAILED);

		verify(userAccountRepository, never()).save(any());
	}

	@Test
	void loginRejectsInvalidCredentials() {
		UserAccount user = UserAccount.registerLocal(
				"jane@example.com",
				"Jane Developer",
				passwordEncoder.encode("password123")
		);
		when(userAccountRepository.findByEmailAndEnabledTrue("jane@example.com")).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.login(new LoginRequest("jane@example.com", "wrong-password")))
				.isInstanceOf(ApiException.class)
				.extracting("code")
				.isEqualTo(ErrorCode.AUTHENTICATION_FAILED);
	}

	@Test
	void loginReturnsTokenPairForValidCredentials() {
		UserAccount user = UserAccount.registerLocal(
				"jane@example.com",
				"Jane Developer",
				passwordEncoder.encode("password123")
		);
		when(userAccountRepository.findByEmailAndEnabledTrue("jane@example.com")).thenReturn(Optional.of(user));
		stubIssuedTokens();

		AuthTokenResponse response = authService.login(new LoginRequest("jane@example.com", "password123"));

		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.refreshToken()).isEqualTo("refresh-token");
	}

	@Test
	void refreshRotatesRefreshTokenAndReturnsNewTokenPair() {
		UserAccount user = UserAccount.registerLocal(
				"jane@example.com",
				"Jane Developer",
				passwordEncoder.encode("password123")
		);
		RefreshToken currentRefreshToken = RefreshToken.issue(
				user,
				"a".repeat(64),
				Instant.parse("2026-05-31T12:00:00Z")
		);
		RefreshTokenService.IssuedRefreshToken nextRefreshToken = new RefreshTokenService.IssuedRefreshToken(
				"next-refresh-token",
				"b".repeat(64),
				Instant.parse("2026-05-31T12:00:00Z"),
				null
		);
		when(refreshTokenService.requireActive("old-refresh-token")).thenReturn(currentRefreshToken);
		when(refreshTokenService.rotate(currentRefreshToken)).thenReturn(nextRefreshToken);
		when(jwtTokenService.issueAccessToken(user)).thenReturn(new JwtTokenService.IssuedAccessToken(
				"next-access-token",
				Instant.parse("2026-05-24T12:15:00Z")
		));

		AuthTokenResponse response = authService.refresh(new RefreshTokenRequest("old-refresh-token"));

		assertThat(response.accessToken()).isEqualTo("next-access-token");
		assertThat(response.refreshToken()).isEqualTo("next-refresh-token");
		verify(refreshTokenService).rotate(currentRefreshToken);
	}

	@Test
	void logoutRevokesSubmittedRefreshToken() {
		authService.logout(new LogoutRequest("refresh-token"));

		verify(refreshTokenService).revoke("refresh-token");
	}

	private void stubIssuedTokens() {
		when(jwtTokenService.issueAccessToken(any(UserAccount.class))).thenReturn(new JwtTokenService.IssuedAccessToken(
				"access-token",
				Instant.parse("2026-05-24T12:15:00Z")
		));
		when(refreshTokenService.issueRefreshToken(any(UserAccount.class))).thenReturn(new RefreshTokenService.IssuedRefreshToken(
				"refresh-token",
				"a".repeat(64),
				Instant.parse("2026-05-31T12:00:00Z"),
				null
		));
	}
}
