package com.applytabai.api.resume.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.resume.storage")
public class ResumeStorageProperties {

	@NotNull
	private Path localRoot = Paths.get("./storage/resumes");

	public Path getLocalRoot() {
		return localRoot;
	}

	public void setLocalRoot(Path localRoot) {
		this.localRoot = localRoot;
	}
}
