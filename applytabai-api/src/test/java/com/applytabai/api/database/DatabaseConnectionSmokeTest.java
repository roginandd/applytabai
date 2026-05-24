package com.applytabai.api.database;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.applytabai.api.applications.domain.ApplicationStatus;
import com.applytabai.api.applications.domain.JobApplication;
import com.applytabai.api.applications.repository.JobApplicationRepository;
import com.applytabai.api.jobs.domain.JobAnalysis;
import com.applytabai.api.jobs.domain.JobLevel;
import com.applytabai.api.jobs.domain.JobPost;
import com.applytabai.api.jobs.domain.JobSource;
import com.applytabai.api.jobs.domain.WorkSetup;
import com.applytabai.api.jobs.repository.JobAnalysisRepository;
import com.applytabai.api.jobs.repository.JobPostRepository;
import com.applytabai.api.preferences.domain.JobPreference;
import com.applytabai.api.preferences.repository.JobPreferenceRepository;
import com.applytabai.api.resume.domain.ResumeFile;
import com.applytabai.api.resume.repository.ResumeFileRepository;
import com.applytabai.api.users.domain.AuthProvider;
import com.applytabai.api.users.domain.UserAccount;
import com.applytabai.api.users.repository.UserAccountRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_LOCAL_DB_SMOKE_TEST", matches = "true")
@SpringBootTest(properties = {
		"spring.config.import=optional:file:.env[.properties]",
		"spring.docker.compose.enabled=false",
		"spring.jpa.hibernate.ddl-auto=update",
		"spring.jpa.open-in-view=false"
})
class DatabaseConnectionSmokeTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private ResumeFileRepository resumeFileRepository;

	@Autowired
	private JobPostRepository jobPostRepository;

	@Autowired
	private JobAnalysisRepository jobAnalysisRepository;

	@Autowired
	private JobApplicationRepository jobApplicationRepository;

	@Autowired
	private JobPreferenceRepository jobPreferenceRepository;

	@Test
	void connectsToConfiguredDatabaseAndPersistsDomainGraph() {
		Integer probe = jdbcTemplate.queryForObject("select 1", Integer.class);
		assertThat(probe).isEqualTo(1);

		String marker = UUID.randomUUID().toString();
		String email = "db-smoke-" + marker + "@example.com";
		String jobUrl = "https://example.com/jobs/" + marker;

		UserAccount user = userAccountRepository.save(
				UserAccount.register(email, "Database Smoke User", AuthProvider.LOCAL_DEV, null)
		);
		ResumeFile resume = resumeFileRepository.save(ResumeFile.upload(
				user,
				"resume.pdf",
				"resume-" + marker + ".pdf",
				"/tmp/applytabai/resume-" + marker + ".pdf",
				ResumeFile.PDF_CONTENT_TYPE,
				2048,
				true
		));
		JobPost jobPost = jobPostRepository.save(JobPost.create(
				user,
				"Backend Intern",
				"ApplyTabAI",
				jobUrl,
				"Remote",
				"Build Spring Boot APIs with PostgreSQL",
				JobSource.OTHER
		));
		JobAnalysis analysis = jobAnalysisRepository.save(JobAnalysis.recordFor(
				jobPost,
				88,
				List.of("Java", "Spring Boot", "PostgreSQL"),
				List.of("Docker"),
				WorkSetup.REMOTE,
				JobLevel.INTERNSHIP,
				List.of("Requires 5+ years experience")
		));
		JobApplication application = JobApplication.track(user, jobPost, resume, ApplicationStatus.READY_TO_APPLY);
		application.markApplied(Instant.parse("2026-05-24T12:00:00Z"));
		jobApplicationRepository.save(application);
		jobPreferenceRepository.save(JobPreference.createFor(
				user,
				List.of("Backend Intern", "Java Developer Intern"),
				List.of("Remote", "Cebu"),
				List.of(WorkSetup.REMOTE, WorkSetup.HYBRID),
				List.of(JobLevel.INTERNSHIP, JobLevel.ENTRY_LEVEL),
				List.of("Java", "Spring Boot", "React", "PostgreSQL"),
				List.of("Senior", "Principal", "Architect")
		));

		UUID userId = user.getId();
		UUID jobPostId = jobPost.getId();

		entityManager.flush();
		entityManager.clear();

		assertThat(userAccountRepository.findByEmail(email))
				.isPresent()
				.get()
				.extracting(UserAccount::getFullName)
				.isEqualTo("Database Smoke User");

		assertThat(resumeFileRepository.findByUser_Id(userId))
				.singleElement()
				.extracting(ResumeFile::getContentType)
				.isEqualTo(ResumeFile.PDF_CONTENT_TYPE);

		assertThat(jobPostRepository.findByJobUrl(jobUrl))
				.isPresent()
				.get()
				.extracting(JobPost::getSource)
				.isEqualTo(JobSource.OTHER);

		assertThat(jobAnalysisRepository.findByJobPost_Id(jobPostId))
				.isPresent()
				.get()
				.satisfies(reloaded -> {
					assertThat(reloaded.getId()).isEqualTo(analysis.getId());
					assertThat(reloaded.getDetectedSkills()).containsExactly("Java", "Spring Boot", "PostgreSQL");
					assertThat(reloaded.getMissingSkills()).containsExactly("Docker");
					assertThat(reloaded.getRedFlags()).containsExactly("Requires 5+ years experience");
				});

		assertThat(jobApplicationRepository.findByUser_IdAndStatus(userId, ApplicationStatus.APPLIED))
				.singleElement()
				.extracting(JobApplication::getAppliedAt)
				.isEqualTo(Instant.parse("2026-05-24T12:00:00Z"));

		assertThat(jobPreferenceRepository.findByUser_Id(userId))
				.isPresent()
				.get()
				.satisfies(preference -> {
					assertThat(preference.getPreferredWorkSetups()).containsExactly(WorkSetup.REMOTE, WorkSetup.HYBRID);
					assertThat(preference.getPreferredSkills()).containsExactly("Java", "Spring Boot", "React", "PostgreSQL");
				});
	}
}
