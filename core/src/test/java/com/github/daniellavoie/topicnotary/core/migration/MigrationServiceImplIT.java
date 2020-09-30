package com.github.daniellavoie.topicnotary.core.migration;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp.OpType;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.resource.PatternType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.daniellavoie.topicnotary.core.CoreIntegrationTest;
import com.github.daniellavoie.topicnotary.core.migration.MigrationDefinition.UpdateType;
import com.github.daniellavoie.topicnotary.core.migration.exception.InvalidMigrationDefinitionException;
import com.github.daniellavoie.topicnotary.core.migration.repository.MigrationEntryKafkaRepository;

public class MigrationServiceImplIT extends CoreIntegrationTest {
	private static final String TOPIC = "it-migration-service";
	private static final int PARTITIONS = 5;
	private static final short REPLICATION_FACTOR = 1;
	private final String RETENTION_MS = String.valueOf(-1);

	@Autowired
	private MigrationTopic migrationTopic;

	@Autowired
	private MigrationServiceImpl migrationService;

	@Autowired
	private MigrationEntryKafkaRepository migrationEntryRepository;

	@Autowired
	private AdminClient adminClient;

	@AfterEach
	@BeforeEach
	private void cleanupTopic() throws InterruptedException, ExecutionException {
		Stream<String> migrationTopics = migrationService.loadMigrationEntries("it", "classpath:kafka/migration")
				.stream().flatMap(migrationEntry -> migrationEntry.getDefinitions().stream())
				.map(migrationDefinition -> migrationDefinition.getTopic()).distinct();

		cleanupTopic(Stream.concat(Stream.of(TOPIC, migrationTopic.getName()), migrationTopics).toArray(String[]::new));

		migrationEntryRepository.initializeTopicIfNeeded();
	}

	private MigrationEntry alter(OpType opType) {
		return definition(new MigrationDefinition(TOPIC, UpdateType.ALTER, null, null,
				Stream.of(
						new ConfigEntry(TopicConfig.CLEANUP_POLICY_CONFIG, OpType.DELETE,
								TopicConfig.CLEANUP_POLICY_COMPACT),
						new ConfigEntry(TopicConfig.RETENTION_MS_CONFIG, opType, RETENTION_MS))

						.collect(Collectors.toSet()),
				null, null));
	}

	@Test
	public void assertUndefinedOpTypes() {
		Assertions.assertThrows(InvalidMigrationDefinitionException.class, () -> migrationService.apply(create(null)));

		Assertions.assertThrows(InvalidMigrationDefinitionException.class,
				() -> migrationService.apply(create(OpType.APPEND)));

		Assertions.assertThrows(InvalidMigrationDefinitionException.class, () -> migrationService.apply(alter(null)));
	}

	@Test
	public void assertApplyUpdate() throws InterruptedException, ExecutionException {
		Assertions.assertThrows(ExecutionException.class, () -> describeTopic());

		migrationService.apply(create(OpType.SET));

		Assertions.assertTrue(describeTopic().isPresent());

		Config config = describeConfig();
		Assertions.assertEquals(TopicConfig.CLEANUP_POLICY_COMPACT,
				config.get(TopicConfig.CLEANUP_POLICY_CONFIG).value());

		migrationService.apply(alter(OpType.SET));

		config = describeConfig();
		Assertions.assertEquals(RETENTION_MS, config.get(TopicConfig.RETENTION_MS_CONFIG).value());
		Assertions.assertEquals(TopicConfig.CLEANUP_POLICY_DELETE,
				config.get(TopicConfig.CLEANUP_POLICY_CONFIG).value());

		migrationService.apply(delete());

		Assertions.assertThrows(ExecutionException.class, () -> describeTopic());
	}

	@Test
	public void assertMigrationOrdering() {
		migrationService.migrate();

		List<MigrationEntry> migrations = migrationEntryRepository.findAll();
		int expectedSize = migrationService.loadMigrationEntries("it", "classpath:kafka/migration").size();

		Assertions.assertEquals(expectedSize, migrations.size());

		MigrationEntry migration1 = migrations.get(0);
		MigrationEntry migration2 = migrations.get(1);
		MigrationEntry migration3 = migrations.get(2);
		MigrationEntry migration4 = migrations.get(3);
		MigrationEntry migration5 = migrations.get(4);
		Assertions.assertEquals("0.0.1-dev.1", migration1.getVersion());
		Assertions.assertEquals("0.0.1-dev.2", migration2.getVersion());
		Assertions.assertEquals("0.1.0", migration3.getVersion());
		Assertions.assertEquals("0.1.1", migration4.getVersion());
		Assertions.assertEquals("0.2.0", migration5.getVersion());
	}

	private MigrationEntry delete() {
		return definition(new MigrationDefinition(TOPIC, UpdateType.DELETE, null, null, null, null, null));
	}

	private Config describeConfig() throws InterruptedException, ExecutionException {
		Collection<Config> configs = adminClient
				.describeConfigs(Arrays.asList(new ConfigResource(ConfigResource.Type.TOPIC, TOPIC))).all().get()
				.values();

		Assertions.assertEquals(1, configs.size());

		return configs.iterator().next();
	}

	private Optional<TopicDescription> describeTopic() throws InterruptedException, ExecutionException {
		return Optional.ofNullable(adminClient.describeTopics(Arrays.asList(TOPIC)).all().get().get(TOPIC));
	}

	private MigrationEntry create(OpType opType) {
		return definition(new MigrationDefinition(TOPIC, UpdateType.CREATE, PARTITIONS, REPLICATION_FACTOR,
				Stream.of(new ConfigEntry(TopicConfig.CLEANUP_POLICY_CONFIG, opType,
						TopicConfig.CLEANUP_POLICY_COMPACT)).collect(Collectors.toSet()),
				Stream.of(
						new AclEntry("User:app", "*", AclOperation.READ, AclPermissionType.ALLOW, PatternType.LITERAL))
						.collect(Collectors.toSet()),
				null));
	}

	private MigrationEntry definition(MigrationDefinition definition) {
		return new MigrationEntry("integration-tests", UUID.randomUUID().toString(), "0.0.1", "Test migration", null,
				Arrays.asList(definition));
	}
}
