package com.applytabai.api.domain;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import com.applytabai.api.applications.domain.ApplicationStatus;
import com.applytabai.api.applications.domain.JobApplication;
import com.applytabai.api.jobs.domain.JobAnalysis;
import com.applytabai.api.jobs.domain.JobLevel;
import com.applytabai.api.jobs.domain.JobPost;
import com.applytabai.api.jobs.domain.JobSource;
import com.applytabai.api.jobs.domain.WorkSetup;
import com.applytabai.api.preferences.domain.JobPreference;
import com.applytabai.api.resume.domain.ResumeFile;
import com.applytabai.api.users.domain.AuthProvider;
import com.applytabai.api.users.domain.UserAccount;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainValidationTest {

	@Test
	void normalizesUserIdentity() {
		UserAccount user = UserAccount.register("  TEST@Example.COM ", "  Jane Developer  ", AuthProvider.LOCAL_DEV, "  local-1  ");

		assertThat(user.getEmail()).isEqualTo("test@example.com");
		assertThat(user.getFullName()).isEqualTo("Jane Developer");
		assertThat(user.getProviderId()).isEqualTo("local-1");
	}

	@Test
	void rejectsInvalidUserIdentity() {
		assertThatThrownBy(() -> UserAccount.register("not-an-email", "Jane Developer", AuthProvider.LOCAL_DEV, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("email");

		assertThatThrownBy(() -> UserAccount.register("jane@example.com", " ", AuthProvider.LOCAL_DEV, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("fullName");
	}

	@Test
	void validatesResumeFileRules() {
		UserAccount user = user("resume@example.com");

		assertThatThrownBy(() -> ResumeFile.upload(
				user,
				"resume.docx",
				"resume.docx",
				"/tmp/resume.docx",
				"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
				1000,
				false
		)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("application/pdf");

		assertThatThrownBy(() -> ResumeFile.upload(
				user,
				"resume.pdf",
				"resume.pdf",
				"/tmp/resume.pdf",
				ResumeFile.PDF_CONTENT_TYPE,
				0,
				false
		)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("fileSize");
	}

	@Test
	void validatesJobPostUrl() {
		UserAccount user = user("job-post@example.com");

		assertThatThrownBy(() -> JobPost.create(
				user,
				"Backend Intern",
				"ApplyTabAI",
				"not-a-url",
				"Remote",
				"Build APIs",
				JobSource.OTHER
		)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("jobUrl");
	}

	@Test
	void validatesJobAnalysisScoreAndJsonItems() {
		JobPost jobPost = jobPost(user("analysis@example.com"));

		assertThatThrownBy(() -> JobAnalysis.recordFor(
				jobPost,
				101,
				List.of("Java"),
				List.of(),
				WorkSetup.REMOTE,
				JobLevel.INTERNSHIP,
				List.of()
		)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("matchScore");

		assertThatThrownBy(() -> JobAnalysis.recordFor(
				jobPost,
				90,
				List.of(" "),
				List.of(),
				WorkSetup.REMOTE,
				JobLevel.INTERNSHIP,
				List.of()
		)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("detectedSkills");
	}

	@Test
	void rejectsApplicationWithMismatchedOwner() {
		UserAccount owner = user("owner@example.com");
		UserAccount other = user("other@example.com");
		JobPost jobPost = jobPost(owner);
		ResumeFile resumeFile = resumeFile(other);

		assertThatThrownBy(() -> JobApplication.track(owner, jobPost, resumeFile, ApplicationStatus.SAVED))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("resumeFile");
	}

	@Test
	void recordsApplicationSubmission() {
		UserAccount owner = user("applied@example.com");
		JobApplication application = JobApplication.track(owner, jobPost(owner), resumeFile(owner));
		Instant appliedAt = Instant.parse("2026-05-24T12:00:00Z");

		application.markApplied(appliedAt);
		application.updateNotes("  Sent through LinkedIn  ");

		assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
		assertThat(application.getAppliedAt()).isEqualTo(appliedAt);
		assertThat(application.getNotes()).isEqualTo("Sent through LinkedIn");
	}

	@Test
	void validatesPreferenceJsonLists() {
		UserAccount owner = user("preferences@example.com");

		JobPreference preference = JobPreference.createFor(
				owner,
				List.of("  Backend Intern  "),
				List.of("Remote"),
				List.of(WorkSetup.REMOTE),
				List.of(JobLevel.INTERNSHIP),
				List.of("Java"),
				List.of("Senior")
		);

		assertThat(preference.getTargetRoles()).containsExactly("Backend Intern");

		assertThatThrownBy(() -> JobPreference.createFor(
				owner,
				List.of("Backend Intern"),
				List.of("Remote"),
				Arrays.asList(WorkSetup.REMOTE, null),
				List.of(JobLevel.INTERNSHIP),
				List.of("Java"),
				List.of("Senior")
		)).isInstanceOf(NullPointerException.class)
				.hasMessageContaining("preferredWorkSetups");
	}

	private UserAccount user(String email) {
		return UserAccount.register(email, "Jane Developer", AuthProvider.LOCAL_DEV, null);
	}

	private ResumeFile resumeFile(UserAccount user) {
		return ResumeFile.upload(
				user,
				"resume.pdf",
				"stored-resume.pdf",
				"/tmp/stored-resume.pdf",
				ResumeFile.PDF_CONTENT_TYPE,
				1000,
				true
		);
	}

	private JobPost jobPost(UserAccount user) {
		return JobPost.create(
				user,
				"Backend Intern",
				"ApplyTabAI",
				"https://example.com/jobs/backend-intern",
				"Remote",
				"Build Spring Boot APIs",
				JobSource.OTHER
		);
	}
}
