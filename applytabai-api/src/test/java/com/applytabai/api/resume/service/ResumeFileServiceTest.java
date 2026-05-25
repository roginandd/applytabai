package com.applytabai.api.resume.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import com.applytabai.api.common.exception.ApiException;
import com.applytabai.api.common.exception.ErrorCode;
import com.applytabai.api.resume.config.ResumeStorageProperties;
import com.applytabai.api.resume.domain.ResumeFile;
import com.applytabai.api.resume.dto.ResumeFileResponse;
import com.applytabai.api.resume.infrastructure.LocalResumeFileStorage;
import com.applytabai.api.resume.repository.ResumeFileRepository;
import com.applytabai.api.resume.service.impl.ResumeFileServiceImpl;
import com.applytabai.api.resume.validation.ResumeFileValidator;
import com.applytabai.api.users.domain.UserAccount;
import com.applytabai.api.users.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeFileServiceTest {

	@TempDir
	private Path storageRoot;

	@Mock
	private UserAccountRepository userAccountRepository;

	@Mock
	private ResumeFileRepository resumeFileRepository;

	private UUID userId;
	private UserAccount user;
	private ResumeFileService resumeFileService;

	@BeforeEach
	void setUp() {
		userId = UUID.randomUUID();
		user = UserAccount.registerLocal("jane@example.com", "Jane Developer", "password-hash");
		ReflectionTestUtils.setField(user, "id", userId);

		ResumeStorageProperties properties = new ResumeStorageProperties();
		properties.setLocalRoot(storageRoot);
		resumeFileService = new ResumeFileServiceImpl(
				userAccountRepository,
				resumeFileRepository,
				new LocalResumeFileStorage(properties),
				new ResumeFileValidator()
		);
	}

	@Test
	void uploadStoresPdfAndMakesFirstResumeDefault() {
		stubAuthenticatedUser();
		when(resumeFileRepository.existsByUser_Id(userId)).thenReturn(false);
		when(resumeFileRepository.saveAndFlush(any(ResumeFile.class))).thenAnswer(invocation -> {
			ResumeFile resumeFile = invocation.getArgument(0);
			ReflectionTestUtils.setField(resumeFile, "id", UUID.randomUUID());
			return resumeFile;
		});

		ResumeFileResponse response = resumeFileService.upload(userId.toString(), pdf("resume.pdf"), false);

		ArgumentCaptor<ResumeFile> resumeCaptor = ArgumentCaptor.forClass(ResumeFile.class);
		verify(resumeFileRepository).saveAndFlush(resumeCaptor.capture());
		ResumeFile savedResume = resumeCaptor.getValue();

		assertThat(response.defaultResume()).isTrue();
		assertThat(savedResume.getOriginalFilename()).isEqualTo("resume.pdf");
		assertThat(savedResume.isDefaultResume()).isTrue();
		assertThat(Files.exists(storageRoot.resolve(savedResume.getStoragePath()))).isTrue();
		verify(resumeFileRepository).clearDefaultForUser(userId);
	}

	@Test
	void uploadRejectsNonPdfContent() {
		stubAuthenticatedUser();
		MockMultipartFile badFile = new MockMultipartFile(
				"file",
				"resume.pdf",
				ResumeFile.PDF_CONTENT_TYPE,
				"not a pdf".getBytes(StandardCharsets.UTF_8)
		);

		assertThatThrownBy(() -> resumeFileService.upload(userId.toString(), badFile, false))
				.isInstanceOf(ApiException.class)
				.extracting("code")
				.isEqualTo(ErrorCode.VALIDATION_FAILED);

		verify(resumeFileRepository, never()).saveAndFlush(any());
	}

	@Test
	void markDefaultClearsPreviousDefaultForUser() {
		stubAuthenticatedUser();
		UUID resumeFileId = UUID.randomUUID();
		ResumeFile resumeFile = resume(false, "stored-resume.pdf");
		ReflectionTestUtils.setField(resumeFile, "id", resumeFileId);

		when(resumeFileRepository.findByIdAndUser_Id(resumeFileId, userId)).thenReturn(Optional.of(resumeFile));
		when(resumeFileRepository.saveAndFlush(resumeFile)).thenReturn(resumeFile);

		ResumeFileResponse response = resumeFileService.markDefault(userId.toString(), resumeFileId);

		assertThat(response.defaultResume()).isTrue();
		assertThat(resumeFile.isDefaultResume()).isTrue();
		verify(resumeFileRepository).clearDefaultForUser(userId);
		verify(resumeFileRepository).saveAndFlush(resumeFile);
	}

	@Test
	void deleteDefaultResumePromotesNewestRemainingResumeAndDeletesLocalFile() throws Exception {
		stubAuthenticatedUser();
		UUID resumeFileId = UUID.randomUUID();
		String storagePath = userId + "/deleted-resume.pdf";
		Path storedFile = storageRoot.resolve(storagePath);
		Files.createDirectories(storedFile.getParent());
		Files.writeString(storedFile, "%PDF- deleted", StandardCharsets.UTF_8);

		ResumeFile deletedResume = resume(true, "deleted-resume.pdf", storagePath);
		ReflectionTestUtils.setField(deletedResume, "id", resumeFileId);
		ResumeFile nextDefault = resume(false, "next-resume.pdf");

		when(resumeFileRepository.findByIdAndUser_Id(resumeFileId, userId)).thenReturn(Optional.of(deletedResume));
		when(resumeFileRepository.findFirstByUser_IdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(nextDefault));
		when(resumeFileRepository.saveAndFlush(nextDefault)).thenReturn(nextDefault);

		resumeFileService.delete(userId.toString(), resumeFileId);

		assertThat(Files.exists(storedFile)).isFalse();
		assertThat(nextDefault.isDefaultResume()).isTrue();
		verify(resumeFileRepository).delete(deletedResume);
		verify(resumeFileRepository).flush();
		verify(resumeFileRepository).saveAndFlush(nextDefault);
	}

	private void stubAuthenticatedUser() {
		when(userAccountRepository.findByIdAndEnabledTrue(userId)).thenReturn(Optional.of(user));
	}

	private ResumeFile resume(boolean defaultResume, String storedFilename) {
		return resume(defaultResume, storedFilename, userId + "/" + storedFilename);
	}

	private ResumeFile resume(boolean defaultResume, String storedFilename, String storagePath) {
		return ResumeFile.upload(
				user,
				"resume.pdf",
				storedFilename,
				storagePath,
				ResumeFile.PDF_CONTENT_TYPE,
				1000,
				defaultResume
		);
	}

	private MockMultipartFile pdf(String originalFilename) {
		return new MockMultipartFile(
				"file",
				originalFilename,
				ResumeFile.PDF_CONTENT_TYPE,
				"%PDF- test resume".getBytes(StandardCharsets.UTF_8)
		);
	}
}
