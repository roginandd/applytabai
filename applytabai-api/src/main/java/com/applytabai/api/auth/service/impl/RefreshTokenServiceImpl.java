package com.applytabai.api.auth.service.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import com.applytabai.api.auth.config.SecurityProperties;
import com.applytabai.api.auth.domain.RefreshToken;
import com.applytabai.api.auth.repository.RefreshTokenRepository;
import com.applytabai.api.auth.service.RefreshTokenService;
import com.applytabai.api.common.exception.ApiException;
import com.applytabai.api.common.exception.ErrorCode;
import com.applytabai.api.common.validation.DomainGuard;
import com.applytabai.api.users.domain.UserAccount;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

	private static final int REFRESH_TOKEN_BYTES = 64;

	private final RefreshTokenRepository refreshTokenRepository;
	private final SecurityProperties securityProperties;
	private final SecureRandom secureRandom = new SecureRandom();
	private final Clock clock;

	public RefreshTokenServiceImpl(
			RefreshTokenRepository refreshTokenRepository,
			SecurityProperties securityProperties,
			Clock clock
	) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.securityProperties = securityProperties;
		this.clock = clock;
	}

	@Override
	public IssuedRefreshToken issueRefreshToken(UserAccount user) {
		String rawToken = generateRawToken();
		String tokenHash = hash(rawToken);
		Instant expiresAt = clock.instant().plus(securityProperties.getJwt().getRefreshTokenTtl());
		RefreshToken refreshToken = refreshTokenRepository.save(RefreshToken.issue(user, tokenHash, expiresAt));
		return new IssuedRefreshToken(rawToken, tokenHash, expiresAt, refreshToken);
	}

	@Override
	public RefreshToken requireActive(String rawToken) {
		String tokenHash = hash(DomainGuard.requireNotBlank(rawToken, "refreshToken"));
		RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
				.orElseThrow(this::invalidRefreshToken);

		if (!refreshToken.isActive(clock.instant())) {
			throw invalidRefreshToken();
		}

		return refreshToken;
	}

	@Override
	public IssuedRefreshToken rotate(RefreshToken currentRefreshToken) {
		IssuedRefreshToken nextRefreshToken = issueRefreshToken(currentRefreshToken.getUser());
		currentRefreshToken.rotateTo(nextRefreshToken.tokenHash(), clock.instant());
		refreshTokenRepository.save(currentRefreshToken);
		return nextRefreshToken;
	}

	@Override
	public void revoke(String rawToken) {
		String tokenHash = hash(DomainGuard.requireNotBlank(rawToken, "refreshToken"));
		refreshTokenRepository.findByTokenHash(tokenHash)
				.ifPresent(refreshToken -> {
					refreshToken.revoke(clock.instant());
					refreshTokenRepository.save(refreshToken);
				});
	}

	private String generateRawToken() {
		byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hash(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 digest is not available", exception);
		}
	}

	private ApiException invalidRefreshToken() {
		return new ApiException(ErrorCode.TOKEN_INVALID, HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired");
	}
}
