package com.example.google_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GoogleBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GoogleBackendApplication.class, args);
	}

}
