package com.pandaterry.concurrent_entity_change_logger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ConcurrentEntityChangeTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConcurrentEntityChangeTrackerApplication.class, args);
	}

}
