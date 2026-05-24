package com.applytabai.api;

import org.springframework.boot.SpringApplication;

public class TestApplyTabAiApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(ApplyTabAiApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
