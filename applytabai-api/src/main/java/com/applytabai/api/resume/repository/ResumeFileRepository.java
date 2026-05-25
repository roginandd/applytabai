package com.applytabai.api.resume.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.applytabai.api.resume.domain.ResumeFile;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResumeFileRepository extends JpaRepository<ResumeFile, UUID> {

	List<ResumeFile> findByUser_Id(UUID userId);

	List<ResumeFile> findByUser_IdOrderByCreatedAtDesc(UUID userId);

	Optional<ResumeFile> findByIdAndUser_Id(UUID resumeFileId, UUID userId);

	Optional<ResumeFile> findFirstByUser_IdAndDefaultResumeTrue(UUID userId);

	Optional<ResumeFile> findFirstByUser_IdOrderByCreatedAtDesc(UUID userId);

	boolean existsByUser_Id(UUID userId);

	@Modifying(flushAutomatically = true)
	@Query("""
			update ResumeFile resumeFile
			set resumeFile.defaultResume = false
			where resumeFile.user.id = :userId
			  and resumeFile.defaultResume = true
			""")
	int clearDefaultForUser(@Param("userId") UUID userId);
}
