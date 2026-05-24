package com.applytabai.api.resume.domain;

import com.applytabai.api.common.persistence.AuditableEntity;
import com.applytabai.api.common.validation.DomainGuard;
import com.applytabai.api.users.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Metadata for a locally stored resume file uploaded by a user.
 */
@Getter
@Entity
@Table(name = "resume_files")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResumeFile extends AuditableEntity {

	public static final String PDF_CONTENT_TYPE = "application/pdf";

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_resume_files_user"))
	private UserAccount user;

	@NotBlank
	@Size(max = 255)
	@Column(name = "original_filename", nullable = false, length = 255)
	private String originalFilename;

	@NotBlank
	@Size(max = 255)
	@Column(name = "stored_filename", nullable = false, length = 255)
	private String storedFilename;

	@NotBlank
	@Column(name = "storage_path", nullable = false, columnDefinition = "text")
	private String storagePath;

	@NotBlank
	@Size(max = 100)
	@Column(name = "content_type", nullable = false, length = 100)
	private String contentType;

	@Positive
	@Column(name = "file_size", nullable = false)
	private long fileSize;

	@Column(name = "is_default", nullable = false)
	private boolean defaultResume;

	private ResumeFile(
			UserAccount user,
			String originalFilename,
			String storedFilename,
			String storagePath,
			String contentType,
			long fileSize,
			boolean defaultResume
	) {
		this.user = DomainGuard.requireNonNull(user, "user");
		updateFileMetadata(originalFilename, storedFilename, storagePath, contentType, fileSize);
		this.defaultResume = defaultResume;
	}

	/**
	 * Creates resume metadata for a PDF stored in the local filesystem.
	 */
	public static ResumeFile upload(
			UserAccount user,
			String originalFilename,
			String storedFilename,
			String storagePath,
			String contentType,
			long fileSize,
			boolean defaultResume
	) {
		return new ResumeFile(user, originalFilename, storedFilename, storagePath, contentType, fileSize, defaultResume);
	}

	/**
	 * Replaces file metadata after the local file has been moved or regenerated.
	 */
	public void updateFileMetadata(
			String originalFilename,
			String storedFilename,
			String storagePath,
			String contentType,
			long fileSize
	) {
		String normalizedContentType = DomainGuard.requireNotBlank(contentType, "contentType");
		if (!PDF_CONTENT_TYPE.equalsIgnoreCase(normalizedContentType)) {
			throw new IllegalArgumentException("contentType must be application/pdf");
		}
		this.originalFilename = DomainGuard.requireNotBlank(originalFilename, "originalFilename");
		this.storedFilename = DomainGuard.requireNotBlank(storedFilename, "storedFilename");
		this.storagePath = DomainGuard.requireNotBlank(storagePath, "storagePath");
		this.contentType = PDF_CONTENT_TYPE;
		this.fileSize = DomainGuard.requirePositive(fileSize, "fileSize");
	}

	/**
	 * Makes this resume the user's preferred resume for new applications.
	 */
	public void markDefault() {
		this.defaultResume = true;
	}

	/**
	 * Removes the default marker when another resume becomes preferred.
	 */
	public void clearDefault() {
		this.defaultResume = false;
	}
}
