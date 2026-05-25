package com.applytabai.api.auth.service;

import java.time.Instant;

import com.applytabai.api.auth.domain.RefreshToken;
import com.applytabai.api.users.domain.UserAccount;

public interface RefreshTokenService {

	IssuedRefreshToken issueRefreshToken(UserAccount user);

	RefreshToken requireActive(String rawToken);

	IssuedRefreshToken rotate(RefreshToken currentRefreshToken);

	void revoke(String rawToken);

	record IssuedRefreshToken(
			String value,
			String tokenHash,
			Instant expiresAt,
			RefreshToken entity
	) {
	}
}
