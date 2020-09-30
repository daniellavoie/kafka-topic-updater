package com.github.daniellavoie.topicnotary.core.migration.repository;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.kafka.clients.admin.AdminClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;

import com.github.daniellavoie.topicnotary.core.Configuration;
import com.github.daniellavoie.topicnotary.core.CoreIntegrationTest;
import com.github.daniellavoie.topicnotary.core.kafka.AclService;
import com.github.daniellavoie.topicnotary.core.kafka.OffsetService;
import com.github.daniellavoie.topicnotary.core.kafka.TopicService;
import com.github.daniellavoie.topicnotary.core.migration.MigrationEntry;
import com.github.daniellavoie.topicnotary.core.migration.MigrationServiceImpl;
import com.github.daniellavoie.topicnotary.core.migration.MigrationTopic;

public class UpdateEntryKafkaRepositoryIT extends CoreIntegrationTest {
	@Autowired
	private AclService aclService;

	@Autowired
	private Configuration configuration;

	@Autowired
	private MigrationTopic migrationTopic;

	@Autowired
	private AdminClient adminClient;

	@Autowired
	private OffsetService offsetService;

	@Autowired
	private TopicService topicService;

	@Autowired
	private KafkaProperties kafkaProperties;

	@AfterEach
	@BeforeEach
	private void cleanupTopic() throws InterruptedException, ExecutionException {
		cleanupTopic(migrationTopic.getName());
	}

	@Test
	public void assertRepositoryOperations() throws InterruptedException {
		Thread.sleep(2000);

		MigrationEntryKafkaRepository repository = new MigrationEntryKafkaRepository(offsetService, migrationTopic,
				adminClient, kafkaProperties.buildConsumerProperties(), kafkaProperties.buildProducerProperties());

		MigrationServiceImpl migrationService = new MigrationServiceImpl(aclService, configuration, repository,
				topicService);

		Assertions.assertEquals(0, repository.findAll().size());

		List<MigrationEntry> migrationEntries = migrationService
				.loadMigrationEntries(configuration.getEnvironmentName(), configuration.getMigrationsDir());

		List<MigrationEntry> resultUpdateEntries = migrationEntries.stream().map(repository::save)
				.collect(Collectors.toList());

		Assertions.assertEquals(migrationEntries.size(), resultUpdateEntries.size());
		compareEntries(migrationEntries, resultUpdateEntries);

		List<MigrationEntry> savedUpdateEntries = repository.findAll();
		Assertions.assertEquals(migrationEntries.size(), savedUpdateEntries.size());
		compareEntries(migrationEntries, savedUpdateEntries);
	}

	private void compareEntries(List<MigrationEntry> expectedUpdateEntries, List<MigrationEntry> migrationEntries) {
		Assertions.assertEquals(expectedUpdateEntries.size(), migrationEntries.size());

		IntStream.range(0, expectedUpdateEntries.size()).boxed()
				.forEach(index -> comparyEntry(expectedUpdateEntries.get(index), migrationEntries.get(index)));
	}

	private void comparyEntry(MigrationEntry expectedUpdateEntry, MigrationEntry migrationEntry) {
		Assertions.assertEquals(expectedUpdateEntry.getChecksum(), migrationEntry.getChecksum());
		Assertions.assertEquals(expectedUpdateEntry.getDescription(), migrationEntry.getDescription());
		Assertions.assertEquals(expectedUpdateEntry.getEnvironment(), migrationEntry.getEnvironment());
		Assertions.assertNull(expectedUpdateEntry.getTimestamp());
		Assertions.assertNull(migrationEntry.getTimestamp());
		Assertions.assertEquals(expectedUpdateEntry.getDefinitions().size(), migrationEntry.getDefinitions().size());

		// TODO - Deep compare update definitions.
	}
}
