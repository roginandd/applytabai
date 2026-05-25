package com.applytabai.api.auth.domain;

import java.time.Instant;

import com.applytabai.api.common.persistence.AuditableEntity;
import com.applytabai.api.common.validation.DomainGuard;
import com.applytabai.api.users.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Persisted server-side refresh-token session.
 */
@Getter
@Entity
@Table(
		name = "refresh_tokens",
		uniqueConstraints = @UniqueConstraint(name = "uk_refresh_tokens_token_hash", columnNames = "token_hash"),
		indexes = {
				@Index(name = "idx_refresh_tokens_user", columnList = "user_id"),
				@Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at")
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends AuditableEntity {

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_refresh_tokens_user"))
	private UserAccount user;

	@NotBlank
	@Size(min = 64, max = 64)
	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@NotNull
	@Future
	@Column(name = "expires_at", nullable = false, columnDefinition = "timestamp with time zone")
	private Instant expiresAt;

	@Column(name = "revoked_at", columnDefinition = "timestamp with time zone")
	private Instant revokedAt;

	@Size(min = 64, max = 64)
	@Column(name = "replaced_by_token_hash", length = 64)
	private String replacedByTokenHash;

	private RefreshToken(UserAccount user, String tokenHash, Instant expiresAt) {
		this.user = DomainGuard.requireNonNull(user, "user");
		this.tokenHash = DomainGuard.requireNotBlank(tokenHash, "tokenHash");
		this.expiresAt = DomainGuard.requireNonNull(expiresAt, "expiresAt");
	}

	public static RefreshToken issue(UserAccount user, String tokenHash, Instant expiresAt) {
		return new RefreshToken(user, tokenHash, expiresAt);
	}

	public boolean isActive(Instant now) {
		return revokedAt == null && expiresAt.isAfter(now);
	}

	public void revoke(Instant now) {
		if (revokedAt == null) {
			revokedAt = DomainGuard.requireNonNull(now, "now");
		}
	}

	public void rotateTo(String replacementTokenHash, Instant now) {
		revoke(now);
		this.replacedByTokenHash = DomainGuard.requireNotBlank(replacementTokenHash, "replacementTokenHash");
	}
}
