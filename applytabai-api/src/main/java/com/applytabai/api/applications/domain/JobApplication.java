package com.applytabai.api.applications.domain;

import java.time.Instant;
import java.util.UUID;

import com.applytabai.api.common.persistence.AuditableEntity;
import com.applytabai.api.common.validation.DomainGuard;
import com.applytabai.api.jobs.domain.JobPost;
import com.applytabai.api.resume.domain.ResumeFile;
import com.applytabai.api.users.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tracks a user's lifecycle for applying to a specific job with a specific resume.
 */
@Getter
@Entity
@Table(
		name = "applications",
		indexes = @Index(name = "idx_applications_status", columnList = "status")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobApplication extends AuditableEntity {

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_applications_user"))
	private UserAccount user;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "job_post_id", nullable = false, foreignKey = @ForeignKey(name = "fk_applications_job_post"))
	private JobPost jobPost;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "resume_file_id", nullable = false, foreignKey = @ForeignKey(name = "fk_applications_resume_file"))
	private ResumeFile resumeFile;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private ApplicationStatus status;

	@Column(columnDefinition = "text")
	private String notes;

	@Column(name = "applied_at", columnDefinition = "timestamp with time zone")
	private Instant appliedAt;

	private JobApplication(UserAccount user, JobPost jobPost, ResumeFile resumeFile, ApplicationStatus status) {
		this.user = DomainGuard.requireNonNull(user, "user");
		this.jobPost = DomainGuard.requireNonNull(jobPost, "jobPost");
		this.resumeFile = DomainGuard.requireNonNull(resumeFile, "resumeFile");
		requireOwnedByUser(user, jobPost.getUser(), "jobPost");
		requireOwnedByUser(user, resumeFile.getUser(), "resumeFile");
		updateStatus(status);
	}

	/**
	 * Starts tracking a saved application draft.
	 */
	public static JobApplication track(UserAccount user, JobPost jobPost, ResumeFile resumeFile) {
		return new JobApplication(user, jobPost, resumeFile, ApplicationStatus.SAVED);
	}

	/**
	 * Starts tracking an application with an explicit pipeline status.
	 */
	public static JobApplication track(
			UserAccount user,
			JobPost jobPost,
			ResumeFile resumeFile,
			ApplicationStatus status
	) {
		return new JobApplication(user, jobPost, resumeFile, status);
	}

	/**
	 * Moves the application through the manual tracking pipeline.
	 */
	public void updateStatus(ApplicationStatus status) {
		this.status = DomainGuard.requireNonNull(status, "status");
	}

	/**
	 * Records the moment the application was actually submitted.
	 */
	public void markApplied(Instant appliedAt) {
		this.status = ApplicationStatus.APPLIED;
		this.appliedAt = DomainGuard.requireNonNull(appliedAt, "appliedAt");
	}

	/**
	 * Replaces optional internal notes.
	 */
	public void updateNotes(String notes) {
		this.notes = DomainGuard.normalizeOptional(notes);
	}

	private void requireOwnedByUser(UserAccount expectedOwner, UserAccount actualOwner, String relationName) {
		DomainGuard.requireNonNull(actualOwner, relationName + ".user");
		if (expectedOwner == actualOwner) {
			return;
		}
		UUID expectedId = expectedOwner.getId();
		UUID actualId = actualOwner.getId();
		if (expectedId != null && expectedId.equals(actualId)) {
			return;
		}
		throw new IllegalArgumentException(relationName + " must belong to the application user");
	}
}
