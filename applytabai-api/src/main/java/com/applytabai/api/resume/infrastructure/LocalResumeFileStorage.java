package com.applytabai.api.resume.infrastructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import com.applytabai.api.common.exception.ApiException;
import com.applytabai.api.common.exception.ErrorCode;
import com.applytabai.api.resume.config.ResumeStorageProperties;
import com.applytabai.api.resume.domain.ResumeFile;
import com.applytabai.api.resume.dto.StoredResumeFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LocalResumeFileStorage implements ResumeFileStorage {

	private final Path localRoot;

	public LocalResumeFileStorage(ResumeStorageProperties properties) {
		this.localRoot = properties.getLocalRoot().toAbsolutePath().normalize();
	}

	@Override
	public StoredResumeFile store(UUID userId, MultipartFile file) {
		String storedFilename = UUID.randomUUID() + ".pdf";
		Path userDirectory = localRoot.resolve(userId.toString()).normalize();
		Path target = userDirectory.resolve(storedFilename).normalize();
		assertUnderRoot(target);

		try {
			Files.createDirectories(userDirectory);
			file.transferTo(target);
			return new StoredResumeFile(
					storedFilename,
					normalizeStoragePath(localRoot.relativize(target)),
					ResumeFile.PDF_CONTENT_TYPE,
					file.getSize()
			);
		} catch (IOException exception) {
			throw storageFailure("Could not store resume file", exception);
		}
	}

	@Override
	public Resource load(String storagePath) {
		Path target = resolve(storagePath);
		if (!Files.isRegularFile(target) || !Files.isReadable(target)) {
			throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Resume file content was not found");
		}
		return new FileSystemResource(target);
	}

	@Override
	public void delete(String storagePath) {
		Path target = resolve(storagePath);
		try {
			Files.deleteIfExists(target);
		} catch (IOException exception) {
			throw storageFailure("Could not delete resume file", exception);
		}
	}

	private Path resolve(String storagePath) {
		String normalizedStoragePath = StringUtils.cleanPath(storagePath);
		Path target = localRoot.resolve(normalizedStoragePath).normalize();
		assertUnderRoot(target);
		return target;
	}

	private void assertUnderRoot(Path target) {
		if (!target.startsWith(localRoot)) {
			throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "Invalid resume storage path");
		}
	}

	private String normalizeStoragePath(Path storagePath) {
		return storagePath.toString().replace('\\', '/');
	}

	private ApiException storageFailure(String message, IOException exception) {
		return new ApiException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, message, exception);
	}
}
