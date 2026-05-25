package com.applytabai.api.users.domain;

import com.applytabai.api.common.persistence.AuditableEntity;
import com.applytabai.api.common.validation.DomainGuard;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * User identity record and owner for all ApplyTabAI job-search data.
 */
@Getter
@Entity
@Table(
		name = "users",
		uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccount extends AuditableEntity {

	@Email
	@NotBlank
	@Size(max = 255)
	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@NotBlank
	@Size(max = 255)
	@Column(name = "full_name", nullable = false, length = 255)
	private String fullName;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private AuthProvider provider;

	@Size(max = 255)
	@Column(name = "provider_id", length = 255)
	private String providerId;

	@Size(max = 255)
	@Column(name = "password_hash", length = 255)
	private String passwordHash;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private UserRole role = UserRole.USER;

	@Column(nullable = false)
	private boolean enabled = true;

	private UserAccount(String email, String fullName, AuthProvider provider, String providerId) {
		changeEmail(email);
		rename(fullName);
		updateProvider(provider, providerId);
	}

	/**
	 * Registers a new user with normalized email and display name values.
	 */
	public static UserAccount register(String email, String fullName, AuthProvider provider, String providerId) {
		return new UserAccount(email, fullName, provider, providerId);
	}

	/**
	 * Registers a local email/password user with a pre-encoded password hash.
	 */
	public static UserAccount registerLocal(String email, String fullName, String passwordHash) {
		UserAccount user = new UserAccount(email, fullName, AuthProvider.LOCAL, null);
		user.changePasswordHash(passwordHash);
		return user;
	}

	/**
	 * Updates the login email while preserving the lower-case canonical form.
	 */
	public void changeEmail(String email) {
		this.email = DomainGuard.normalizeEmail(email);
	}

	/**
	 * Renames the user-facing display name.
	 */
	public void rename(String fullName) {
		this.fullName = DomainGuard.requireNotBlank(fullName, "fullName");
	}

	/**
	 * Replaces the provider metadata received from the authentication source.
	 */
	public void updateProvider(AuthProvider provider, String providerId) {
		this.provider = DomainGuard.requireNonNull(provider, "provider");
		this.providerId = DomainGuard.normalizeOptional(providerId);
	}

	/**
	 * Replaces the encoded local password hash.
	 */
	public void changePasswordHash(String passwordHash) {
		this.passwordHash = DomainGuard.requireNotBlank(passwordHash, "passwordHash");
	}

	public boolean hasLocalPassword() {
		return passwordHash != null && !passwordHash.isBlank();
	}

	public void disable() {
		this.enabled = false;
	}
}
