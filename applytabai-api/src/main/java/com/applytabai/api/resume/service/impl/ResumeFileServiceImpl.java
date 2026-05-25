package com.applytabai.api.resume.service.impl;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.applytabai.api.common.exception.ApiException;
import com.applytabai.api.common.exception.ErrorCode;
import com.applytabai.api.resume.domain.ResumeFile;
import com.applytabai.api.resume.dto.ResumeFileDownload;
import com.applytabai.api.resume.dto.ResumeFileResponse;
import com.applytabai.api.resume.dto.StoredResumeFile;
import com.applytabai.api.resume.infrastructure.ResumeFileStorage;
import com.applytabai.api.resume.repository.ResumeFileRepository;
import com.applytabai.api.resume.service.ResumeFileService;
import com.applytabai.api.resume.validation.ResumeFileValidator;
import com.applytabai.api.users.domain.UserAccount;
import com.applytabai.api.users.repository.UserAccountRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeFileServiceImpl implements ResumeFileService {

	private final UserAccountRepository userAccountRepository;
	private final ResumeFileRepository resumeFileRepository;
	private final ResumeFileStorage resumeFileStorage;
	private final ResumeFileValidator resumeFileValidator;

	public ResumeFileServiceImpl(
			UserAccountRepository userAccountRepository,
			ResumeFileRepository resumeFileRepository,
			ResumeFileStorage resumeFileStorage,
			ResumeFileValidator resumeFileValidator
	) {
		this.userAccountRepository = userAccountRepository;
		this.resumeFileRepository = resumeFileRepository;
		this.resumeFileStorage = resumeFileStorage;
		this.resumeFileValidator = resumeFileValidator;
	}

	@Override
	@Transactional
	public ResumeFileResponse upload(String subject, MultipartFile file, boolean defaultResume) {
		UserAccount user = requireUser(subject);
		UUID userId = user.getId();
		resumeFileValidator.validatePdf(file);

		boolean shouldMakeDefault = defaultResume || !resumeFileRepository.existsByUser_Id(userId);
		if (shouldMakeDefault) {
			resumeFileRepository.clearDefaultForUser(userId);
		}

		StoredResumeFile storedFile = resumeFileStorage.store(userId, file);
		ResumeFile resumeFile = ResumeFile.upload(
				user,
				resumeFileValidator.normalizedOriginalFilename(file),
				storedFile.storedFilename(),
				storedFile.storagePath(),
				storedFile.contentType(),
				storedFile.fileSize(),
				shouldMakeDefault
		);

		try {
			return ResumeFileResponse.from(resumeFileRepository.saveAndFlush(resumeFile));
		} catch (RuntimeException exception) {
			deleteStoredFileAfterFailedMetadataSave(storedFile.storagePath(), exception);
			throw exception;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<ResumeFileResponse> list(String subject) {
		UUID userId = requireUser(subject).getId();
		return resumeFileRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
				.map(ResumeFileResponse::from)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ResumeFileResponse getDefault(String subject) {
		UUID userId = requireUser(subject).getId();
		return resumeFileRepository.findFirstByUser_IdAndDefaultResumeTrue(userId)
				.map(ResumeFileResponse::from)
				.orElseThrow(() -> notFound("Default resume file was not found"));
	}

	@Override
	@Transactional(readOnly = true)
	public ResumeFileDownload download(String subject, UUID resumeFileId) {
		UUID userId = requireUser(subject).getId();
		ResumeFile resumeFile = requireOwnedResume(userId, resumeFileId);
		Resource resource = resumeFileStorage.load(resumeFile.getStoragePath());
		return new ResumeFileDownload(
				resumeFile.getOriginalFilename(),
				resumeFile.getContentType(),
				contentLength(resource),
				resource
		);
	}

	@Override
	@Transactional
	public ResumeFileResponse markDefault(String subject, UUID resumeFileId) {
		UUID userId = requireUser(subject).getId();
		ResumeFile resumeFile = requireOwnedResume(userId, resumeFileId);
		if (resumeFile.isDefaultResume()) {
			return ResumeFileResponse.from(resumeFile);
		}

		resumeFileRepository.clearDefaultForUser(userId);
		resumeFile.markDefault();
		return ResumeFileResponse.from(resumeFileRepository.saveAndFlush(resumeFile));
	}

	@Override
	@Transactional
	public void delete(String subject, UUID resumeFileId) {
		UUID userId = requireUser(subject).getId();
		ResumeFile resumeFile = requireOwnedResume(userId, resumeFileId);
		boolean wasDefault = resumeFile.isDefaultResume();
		String storagePath = resumeFile.getStoragePath();

		resumeFileRepository.delete(resumeFile);
		resumeFileRepository.flush();

		if (wasDefault) {
			resumeFileRepository.findFirstByUser_IdOrderByCreatedAtDesc(userId)
					.ifPresent(nextDefault -> {
						nextDefault.markDefault();
						resumeFileRepository.saveAndFlush(nextDefault);
					});
		}

		resumeFileStorage.delete(storagePath);
	}

	private UserAccount requireUser(String subject) {
		UUID userId;
		try {
			userId = UUID.fromString(subject);
		} catch (IllegalArgumentException exception) {
			throw new ApiException(
					ErrorCode.AUTHENTICATION_FAILED,
					HttpStatus.UNAUTHORIZED,
					"Authentication is invalid"
			);
		}

		return userAccountRepository.findByIdAndEnabledTrue(userId)
				.orElseThrow(() -> new ApiException(
						ErrorCode.AUTHENTICATION_FAILED,
						HttpStatus.UNAUTHORIZED,
						"Authentication is invalid"
				));
	}

	private ResumeFile requireOwnedResume(UUID userId, UUID resumeFileId) {
		return resumeFileRepository.findByIdAndUser_Id(resumeFileId, userId)
				.orElseThrow(() -> notFound("Resume file was not found"));
	}

	private long contentLength(Resource resource) {
		try {
			return resource.contentLength();
		} catch (IOException exception) {
			throw new ApiException(
					ErrorCode.RESOURCE_NOT_FOUND,
					HttpStatus.NOT_FOUND,
					"Resume file content was not found",
					exception
			);
		}
	}

	private void deleteStoredFileAfterFailedMetadataSave(String storagePath, RuntimeException originalException) {
		try {
			resumeFileStorage.delete(storagePath);
		} catch (RuntimeException cleanupException) {
			originalException.addSuppressed(cleanupException);
		}
	}

	private ApiException notFound(String message) {
		return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, message);
	}
}
