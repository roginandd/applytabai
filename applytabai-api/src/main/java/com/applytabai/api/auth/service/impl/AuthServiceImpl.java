package com.applytabai.api.auth.service.impl;

import java.util.Objects;
import java.util.UUID;

import com.applytabai.api.auth.domain.RefreshToken;
import com.applytabai.api.auth.dto.AuthTokenResponse;
import com.applytabai.api.auth.dto.LoginRequest;
import com.applytabai.api.auth.dto.LogoutRequest;
import com.applytabai.api.auth.dto.RefreshTokenRequest;
import com.applytabai.api.auth.dto.RegisterRequest;
import com.applytabai.api.auth.dto.UserSummaryResponse;
import com.applytabai.api.auth.service.AuthService;
import com.applytabai.api.auth.service.JwtTokenService;
import com.applytabai.api.auth.service.RefreshTokenService;
import com.applytabai.api.common.exception.ApiException;
import com.applytabai.api.common.exception.ErrorCode;
import com.applytabai.api.common.validation.DomainGuard;
import com.applytabai.api.users.domain.UserAccount;
import com.applytabai.api.users.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

	private final UserAccountRepository userAccountRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;
	private final RefreshTokenService refreshTokenService;

	public AuthServiceImpl(
			UserAccountRepository userAccountRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenService jwtTokenService,
			RefreshTokenService refreshTokenService
	) {
		this.userAccountRepository = userAccountRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
		this.refreshTokenService = refreshTokenService;
	}

	@Override
	@Transactional
	public AuthTokenResponse register(RegisterRequest request) {
		if (!Objects.equals(request.password(), request.confirmPassword())) {
			throw new ApiException(
					ErrorCode.VALIDATION_FAILED,
					HttpStatus.BAD_REQUEST,
					"Password confirmation does not match"
			);
		}

		String email = DomainGuard.normalizeEmail(request.email());
		if (userAccountRepository.existsByEmail(email)) {
			throw emailAlreadyRegistered();
		}

		UserAccount user = UserAccount.registerLocal(
				email,
				request.fullName(),
				passwordEncoder.encode(request.password())
		);

		try {
			UserAccount savedUser = userAccountRepository.save(user);
			return issueTokenPair(savedUser);
		} catch (DataIntegrityViolationException exception) {
			throw emailAlreadyRegistered();
		}
	}

	@Override
	@Transactional
	public AuthTokenResponse login(LoginRequest request) {
		String email = DomainGuard.normalizeEmail(request.email());
		UserAccount user = userAccountRepository.findByEmailAndEnabledTrue(email)
				.orElseThrow(this::invalidCredentials);

		if (!user.hasLocalPassword() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw invalidCredentials();
		}

		return issueTokenPair(user);
	}

	@Override
	@Transactional
	public AuthTokenResponse refresh(RefreshTokenRequest request) {
		RefreshToken currentRefreshToken = refreshTokenService.requireActive(request.refreshToken());
		UserAccount user = currentRefreshToken.getUser();
		if (!user.isEnabled()) {
			throw invalidRefreshToken();
		}

		RefreshTokenService.IssuedRefreshToken nextRefreshToken = refreshTokenService.rotate(currentRefreshToken);
		return issueTokenPair(user, nextRefreshToken);
	}

	@Override
	@Transactional
	public void logout(LogoutRequest request) {
		refreshTokenService.revoke(request.refreshToken());
	}

	@Override
	@Transactional(readOnly = true)
	public UserSummaryResponse currentUser(String subject) {
		UUID userId;
		try {
			userId = UUID.fromString(subject);
		} catch (IllegalArgumentException exception) {
			throw new ApiException(
					ErrorCode.AUTHENTICATION_FAILED,
					HttpStatus.UNAUTHORIZED,
					"Authentication is invalid"
			);
		}

		UserAccount user = userAccountRepository.findByIdAndEnabledTrue(userId)
				.orElseThrow(() -> new ApiException(
						ErrorCode.AUTHENTICATION_FAILED,
						HttpStatus.UNAUTHORIZED,
						"Authentication is invalid"
				));

		return UserSummaryResponse.from(user);
	}

	private AuthTokenResponse issueTokenPair(UserAccount user) {
		RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issueRefreshToken(user);
		return issueTokenPair(user, refreshToken);
	}

	private AuthTokenResponse issueTokenPair(
			UserAccount user,
			RefreshTokenService.IssuedRefreshToken refreshToken
	) {
		JwtTokenService.IssuedAccessToken accessToken = jwtTokenService.issueAccessToken(user);
		return new AuthTokenResponse(
				"Bearer",
				accessToken.value(),
				accessToken.expiresAt(),
				refreshToken.value(),
				refreshToken.expiresAt(),
				UserSummaryResponse.from(user)
		);
	}

	private ApiException emailAlreadyRegistered() {
		return new ApiException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Email is already registered");
	}

	private ApiException invalidCredentials() {
		return new ApiException(
				ErrorCode.AUTHENTICATION_FAILED,
				HttpStatus.UNAUTHORIZED,
				"Invalid email or password"
		);
	}

	private ApiException invalidRefreshToken() {
		return new ApiException(ErrorCode.TOKEN_INVALID, HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired");
	}
}
