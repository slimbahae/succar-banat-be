package com.slimbahael.beauty_center;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BeautyCenterApplication {

	public static void main(String[] args) {
		SpringApplication.run(BeautyCenterApplication.class, args);
	}

}
