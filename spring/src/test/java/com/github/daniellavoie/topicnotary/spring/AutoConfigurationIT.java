package com.github.daniellavoie.topicnotary.spring;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.github.daniellavoie.topicnotary.core.migration.MigrationService;

@SpringBootTest(classes = TopicNotaryTestApplication.class)
public class AutoConfigurationIT {
	@Autowired
	private Optional<MigrationService> optionalMigrationService;

	@Test
	public void contextLoads() {
		Assertions.assertTrue(optionalMigrationService.isPresent());
	}
}
