package com.applytabai.api.jobs.repository;

import java.util.Optional;
import java.util.UUID;

import com.applytabai.api.jobs.domain.JobPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostRepository extends JpaRepository<JobPost, UUID> {

	Optional<JobPost> findByJobUrl(String jobUrl);
}
