package com.github.daniellavoie.topicnotary.core.migration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.daniellavoie.topicnotary.core.Configuration;
import com.github.daniellavoie.topicnotary.core.kafka.AclService;
import com.github.daniellavoie.topicnotary.core.kafka.TopicService;
import com.github.daniellavoie.topicnotary.core.migration.MigrationDefinition.UpdateType;
import com.github.daniellavoie.topicnotary.core.migration.exception.ChecksumUtil;
import com.github.daniellavoie.topicnotary.core.migration.exception.InvalidMigrationDefinitionException;
import com.github.daniellavoie.topicnotary.core.migration.repository.MigrationEntryRepository;

public class MigrationServiceImpl implements MigrationService {
	private static final Logger LOGGER = LoggerFactory.getLogger(MigrationServiceImpl.class);

	private final AclService aclService;
	private final Configuration configuration;
	private final MigrationEntryRepository repository;
	private final TopicService topicService;

	public MigrationServiceImpl(AclService aclService, Configuration configuration, MigrationEntryRepository repository,
			TopicService topicService) {
		assertNotEmpty(configuration.getEnvironmentName(), "an environment needs to be specified for the migration");
		assertNotEmpty(configuration.getMigrationsDir(),
				"a path containing the migration manifests needs to be defined");

		this.aclService = aclService;
		this.configuration = configuration;
		this.repository = repository;
		this.topicService = topicService;
	}

	private void apply(MigrationDefinition definition) {
		if (UpdateType.CREATE.equals(definition.getUpdateType())) {
			topicService.createTopic(definition);
		} else if (UpdateType.ALTER.equals(definition.getUpdateType())) {
			topicService.alterTopic(definition);
		} else if (UpdateType.DELETE.equals(definition.getUpdateType())) {
			topicService.deleteTopic(definition);
		} else {
			throw new InvalidMigrationDefinitionException("type is undefined");
		}

		Optional.ofNullable(definition.getCreateAcls())
				.ifPresent(acls -> aclService.createAcls(definition.getTopic(), acls));

		Optional.ofNullable(definition.getDeleteAcls())
				.ifPresent(acls -> aclService.deleteAcls(definition.getTopic(), acls));
	}

	public MigrationEntry apply(MigrationEntry migrationEntry) {
		migrationEntry.getDefinitions().forEach(this::apply);

		MigrationEntry result = repository.save(successfulMigration(migrationEntry));

		LOGGER.info("Successfully applied migration {} - {}.", migrationEntry.getVersion(),
				migrationEntry.getDescription());

		return result;
	}

	public List<MigrationEntry> findAll() {
		return repository.findAll();
	}

	public List<MigrationEntry> loadMigrationEntries(String environment, String migrationDir) {
		return MigrationEntryUtil.loadMigrationEntries(environment, migrationDir);
	}

	@Override
	public void migrate() {
		List<MigrationEntry> existingUpdateEntries = findAll();
		List<MigrationEntry> migrationEntries = loadMigrationEntries(configuration.getEnvironmentName(),
				configuration.getMigrationsDir());

		// Assert that all old updates matches with version and checksum.
		ChecksumUtil.compareExistingUpdates(existingUpdateEntries, migrationEntries)

				.forEach(this::apply);

		LOGGER.info("Completed topics migration.");
	}

	private MigrationEntry successfulMigration(MigrationEntry migrationEntry) {
		return new MigrationEntry(migrationEntry.getEnvironment(), migrationEntry.getChecksum(),
				migrationEntry.getVersion(), migrationEntry.getDescription(), LocalDateTime.now(),
				migrationEntry.getDefinitions());
	}

	private boolean isUndefined(String nullableValue) {
		return !Optional.ofNullable(nullableValue).filter(value -> !value.trim().isEmpty()).isPresent();
	}

	private void assertNotEmpty(String nullableValue, String message) {
		if (isUndefined(nullableValue)) {
			throw new IllegalArgumentException(message);
		}
	}
}
