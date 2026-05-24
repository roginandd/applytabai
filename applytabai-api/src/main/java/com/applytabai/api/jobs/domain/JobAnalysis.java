package com.applytabai.api.jobs.domain;

import java.util.Collection;
import java.util.List;

import com.applytabai.api.common.persistence.CreationTrackedEntity;
import com.applytabai.api.common.validation.DomainGuard;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Stored interpretation of a job description for matching and risk review.
 */
@Getter
@Entity
@Table(
		name = "job_analyses",
		uniqueConstraints = @UniqueConstraint(name = "uk_job_analyses_job_post", columnNames = "job_post_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobAnalysis extends CreationTrackedEntity {

	@NotNull
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "job_post_id", nullable = false, foreignKey = @ForeignKey(name = "fk_job_analyses_job_post"))
	private JobPost jobPost;

	@Min(0)
	@Max(100)
	@Column(name = "match_score", nullable = false)
	private int matchScore;

	@NotNull
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "detected_skills", nullable = false, columnDefinition = "jsonb")
	private List<@NotBlank String> detectedSkills = List.of();

	@NotNull
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "missing_skills", nullable = false, columnDefinition = "jsonb")
	private List<@NotBlank String> missingSkills = List.of();

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "work_setup", nullable = false, length = 50)
	private WorkSetup workSetup;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "job_level", nullable = false, length = 50)
	private JobLevel jobLevel;

	@NotNull
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "red_flags", nullable = false, columnDefinition = "jsonb")
	private List<@NotBlank String> redFlags = List.of();

	private JobAnalysis(
			JobPost jobPost,
			int matchScore,
			Collection<String> detectedSkills,
			Collection<String> missingSkills,
			WorkSetup workSetup,
			JobLevel jobLevel,
			Collection<String> redFlags
	) {
		this.jobPost = DomainGuard.requireNonNull(jobPost, "jobPost");
		replaceFindings(matchScore, detectedSkills, missingSkills, workSetup, jobLevel, redFlags);
	}

	/**
	 * Records the current analysis result for a job post.
	 */
	public static JobAnalysis recordFor(
			JobPost jobPost,
			int matchScore,
			Collection<String> detectedSkills,
			Collection<String> missingSkills,
			WorkSetup workSetup,
			JobLevel jobLevel,
			Collection<String> redFlags
	) {
		return new JobAnalysis(jobPost, matchScore, detectedSkills, missingSkills, workSetup, jobLevel, redFlags);
	}

	/**
	 * Replaces computed analysis values as the parser improves or source text changes.
	 */
	public void replaceFindings(
			int matchScore,
			Collection<String> detectedSkills,
			Collection<String> missingSkills,
			WorkSetup workSetup,
			JobLevel jobLevel,
			Collection<String> redFlags
	) {
		this.matchScore = DomainGuard.requireRange(matchScore, 0, 100, "matchScore");
		this.detectedSkills = DomainGuard.copyStringList(detectedSkills, "detectedSkills");
		this.missingSkills = DomainGuard.copyStringList(missingSkills, "missingSkills");
		this.workSetup = DomainGuard.requireNonNull(workSetup, "workSetup");
		this.jobLevel = DomainGuard.requireNonNull(jobLevel, "jobLevel");
		this.redFlags = DomainGuard.copyStringList(redFlags, "redFlags");
	}
}
