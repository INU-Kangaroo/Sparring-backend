package com.kangaroo.sparring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class SparringApplication {

	public static void main(String[] args) {
		SpringApplication.run(SparringApplication.class, args);
	}

}
