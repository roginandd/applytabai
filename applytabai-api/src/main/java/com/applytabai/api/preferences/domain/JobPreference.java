package com.applytabai.api.preferences.domain;

import java.util.Collection;
import java.util.List;

import com.applytabai.api.common.persistence.AuditableEntity;
import com.applytabai.api.common.validation.DomainGuard;
import com.applytabai.api.jobs.domain.JobLevel;
import com.applytabai.api.jobs.domain.WorkSetup;
import com.applytabai.api.users.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * User-owned configuration for matching and filtering job opportunities.
 */
@Getter
@Entity
@Table(
		name = "job_preferences",
		uniqueConstraints = @UniqueConstraint(name = "uk_job_preferences_user", columnNames = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobPreference extends AuditableEntity {

	@NotNull
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_job_preferences_user"))
	private UserAccount user;

	@NotNull
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "target_roles", nullable = false, columnDefinition = "jsonb")
	private List<@NotBlank String> targetRoles = List.of();

	@NotNull
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "preferred_locations", nullable = false, columnDefinition = "jsonb")
	private List<@NotBlank String> preferredLocations = List.of();

	@NotNull
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "preferred_work_setups", nullable = false, columnDefinition = "jsonb")
	private List<@NotNull WorkSetup> preferredWorkSetups = List.of();

	@NotNull
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "preferred_job_levels", nullable = false, columnDefinition = "jsonb")
	private List<@NotNull JobLevel> preferredJobLevels = List.of();

	@NotNull
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "preferred_skills", nullable = false, columnDefinition = "jsonb")
	private List<@NotBlank String> preferredSkills = List.of();

	@NotNull
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "excluded_keywords", nullable = false, columnDefinition = "jsonb")
	private List<@NotBlank String> excludedKeywords = List.of();

	private JobPreference(
			UserAccount user,
			Collection<String> targetRoles,
			Collection<String> preferredLocations,
			Collection<WorkSetup> preferredWorkSetups,
			Collection<JobLevel> preferredJobLevels,
			Collection<String> preferredSkills,
			Collection<String> excludedKeywords
	) {
		this.user = DomainGuard.requireNonNull(user, "user");
		updatePreferences(
				targetRoles,
				preferredLocations,
				preferredWorkSetups,
				preferredJobLevels,
				preferredSkills,
				excludedKeywords
		);
	}

	/**
	 * Creates the initial matching preferences for a user.
	 */
	public static JobPreference createFor(
			UserAccount user,
			Collection<String> targetRoles,
			Collection<String> preferredLocations,
			Collection<WorkSetup> preferredWorkSetups,
			Collection<JobLevel> preferredJobLevels,
			Collection<String> preferredSkills,
			Collection<String> excludedKeywords
	) {
		return new JobPreference(
				user,
				targetRoles,
				preferredLocations,
				preferredWorkSetups,
				preferredJobLevels,
				preferredSkills,
				excludedKeywords
		);
	}

	/**
	 * Replaces the user's matching preferences in a single consistent update.
	 */
	public void updatePreferences(
			Collection<String> targetRoles,
			Collection<String> preferredLocations,
			Collection<WorkSetup> preferredWorkSetups,
			Collection<JobLevel> preferredJobLevels,
			Collection<String> preferredSkills,
			Collection<String> excludedKeywords
	) {
		this.targetRoles = DomainGuard.copyStringList(targetRoles, "targetRoles");
		this.preferredLocations = DomainGuard.copyStringList(preferredLocations, "preferredLocations");
		this.preferredWorkSetups = DomainGuard.copyEnumList(preferredWorkSetups, "preferredWorkSetups");
		this.preferredJobLevels = DomainGuard.copyEnumList(preferredJobLevels, "preferredJobLevels");
		this.preferredSkills = DomainGuard.copyStringList(preferredSkills, "preferredSkills");
		this.excludedKeywords = DomainGuard.copyStringList(excludedKeywords, "excludedKeywords");
	}
}
