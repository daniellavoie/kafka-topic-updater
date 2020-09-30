package com.github.daniellavoie.topicnotary.spring;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.github.daniellavoie.topicnotary.core.migration.MigrationService;

@SpringBootTest(classes = TopicNotaryTestApplication.class, properties = "topic-notary.enabled=false")
public class DisableTopicNotaryTest {
	@Autowired
	private Optional<MigrationService> optionalMigrationService;

	@Test
	public void assertBeanIsNotConfigured() {
		Assertions.assertFalse(optionalMigrationService.isPresent());
	}
}
