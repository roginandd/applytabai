package com.applytabai.api.auth.service.impl;

import java.time.Clock;
import java.time.Instant;

import com.applytabai.api.auth.config.SecurityProperties;
import com.applytabai.api.auth.service.JwtTokenService;
import com.applytabai.api.users.domain.UserAccount;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenServiceImpl implements JwtTokenService {

	private static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS256;

	private final JwtEncoder jwtEncoder;
	private final SecurityProperties securityProperties;
	private final Clock clock;

	public JwtTokenServiceImpl(JwtEncoder jwtEncoder, SecurityProperties securityProperties, Clock clock) {
		this.jwtEncoder = jwtEncoder;
		this.securityProperties = securityProperties;
		this.clock = clock;
	}

	@Override
	public IssuedAccessToken issueAccessToken(UserAccount user) {
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plus(securityProperties.getJwt().getAccessTokenTtl());

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(securityProperties.getJwt().getIssuer())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.subject(user.getId().toString())
				.claim("email", user.getEmail())
				.claim("role", user.getRole().name())
				.build();

		JwsHeader header = JwsHeader.with(JWT_ALGORITHM).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

		return new IssuedAccessToken(token, expiresAt);
	}
}
