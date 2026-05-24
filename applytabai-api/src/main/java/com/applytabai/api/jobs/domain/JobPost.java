package com.applytabai.api.jobs.domain;

import com.applytabai.api.common.persistence.AuditableEntity;
import com.applytabai.api.common.validation.DomainGuard;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * User-owned job post captured from LinkedIn or another external source.
 */
@Getter
@Entity
@Table(
		name = "job_posts",
		indexes = @Index(name = "idx_job_posts_job_url", columnList = "job_url")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobPost extends AuditableEntity {

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_job_posts_user"))
	private UserAccount user;

	@NotBlank
	@Size(max = 255)
	@Column(nullable = false, length = 255)
	private String title;

	@NotBlank
	@Size(max = 255)
	@Column(nullable = false, length = 255)
	private String company;

	@NotBlank
	@Column(name = "job_url", nullable = false, columnDefinition = "text")
	private String jobUrl;

	@Size(max = 255)
	@Column(length = 255)
	private String location;

	@NotBlank
	@Column(nullable = false, columnDefinition = "text")
	private String description;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private JobSource source;

	private JobPost(
			UserAccount user,
			String title,
			String company,
			String jobUrl,
			String location,
			String description,
			JobSource source
	) {
		this.user = DomainGuard.requireNonNull(user, "user");
		updateDetails(title, company, jobUrl, location, description, source);
	}

	/**
	 * Captures a job post for a user's application pipeline.
	 */
	public static JobPost create(
			UserAccount user,
			String title,
			String company,
			String jobUrl,
			String location,
			String description,
			JobSource source
	) {
		return new JobPost(user, title, company, jobUrl, location, description, source);
	}

	/**
	 * Updates mutable job details after the user edits or refreshes the post.
	 */
	public void updateDetails(
			String title,
			String company,
			String jobUrl,
			String location,
			String description,
			JobSource source
	) {
		this.title = DomainGuard.requireNotBlank(title, "title");
		this.company = DomainGuard.requireNotBlank(company, "company");
		this.jobUrl = DomainGuard.requireHttpUrl(jobUrl, "jobUrl");
		this.location = DomainGuard.normalizeOptional(location);
		this.description = DomainGuard.requireNotBlank(description, "description");
		this.source = DomainGuard.requireNonNull(source, "source");
	}
}
