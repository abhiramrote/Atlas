package com.abhiram.atlas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class AtlasApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AtlasApiApplication.class, args);
	}

}
