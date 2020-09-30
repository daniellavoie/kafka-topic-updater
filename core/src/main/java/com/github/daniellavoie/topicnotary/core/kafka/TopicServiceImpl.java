package com.github.daniellavoie.topicnotary.core.kafka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.AlterConfigOp.OpType;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.ConfigResource.Type;
import org.apache.kafka.common.errors.TopicExistsException;

import com.github.daniellavoie.topicnotary.core.migration.ConfigEntry;
import com.github.daniellavoie.topicnotary.core.migration.MigrationDefinition;
import com.github.daniellavoie.topicnotary.core.migration.exception.InvalidMigrationDefinitionException;

public class TopicServiceImpl implements TopicService {
	private static final List<OpType> ALLOWED_ALTER_TYPE = Arrays.stream(OpType.values()).collect(Collectors.toList());
	private static final List<OpType> ALLOWED_CREATE_TYPES = Arrays.asList(OpType.SET);

	private final AdminClient adminClient;

	public TopicServiceImpl(AdminClient adminClient) {
		this.adminClient = adminClient;

	}

	@Override
	public void alterTopic(MigrationDefinition migrationDefinition) {
		validateOpTypes(migrationDefinition, ALLOWED_ALTER_TYPE);

		Map<ConfigResource, Collection<AlterConfigOp>> configs = new HashMap<>();

		configs.put(new ConfigResource(Type.TOPIC, migrationDefinition.getTopic()),
				alterConfigOps(migrationDefinition));

		// TODO check if replication or partition change requires a special API.

		try {
			adminClient.incrementalAlterConfigs(configs).all().get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	private List<AlterConfigOp> alterConfigOps(MigrationDefinition migrationDefinition) {
		return Optional.ofNullable(migrationDefinition.getConfigs()).map(configs -> configs.stream()

				.map(configUpdate -> new AlterConfigOp(new org.apache.kafka.clients.admin.ConfigEntry(
						configUpdate.getConfig(), configUpdate.getValue()), configUpdate.getType()))

				.collect(Collectors.toList())).orElseGet(() -> new ArrayList<>());
	}

	@Override
	public void createTopic(MigrationDefinition migrationDefinition) {
		validateOpTypes(migrationDefinition, ALLOWED_CREATE_TYPES);

		try {
			adminClient.createTopics(newTopics(migrationDefinition)).all().get();
		} catch (InterruptedException | ExecutionException e) {
			if (e.getCause() == null || !(e.getCause() instanceof TopicExistsException)) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void deleteTopic(MigrationDefinition migrationDefinition) {
		adminClient.deleteTopics(Arrays.asList(migrationDefinition.getTopic()));
	}

	private Collection<NewTopic> newTopics(MigrationDefinition migrationDefinition) {
		NewTopic topic = new NewTopic(migrationDefinition.getTopic(), migrationDefinition.getPartitions(),
				migrationDefinition.getReplicationFactor());

		topic.configs(topicConfigs(migrationDefinition));

		return Arrays.asList(topic);
	}

	private Map<String, String> topicConfigs(MigrationDefinition migrationDefinition) {
		return Optional
				.ofNullable(
						migrationDefinition.getConfigs())
				.map(configs -> configs.stream().collect(Collectors.toMap(configUpdate -> configUpdate.getConfig(),
						configUpdate -> configUpdate.getValue())))
				.orElseGet(() -> new HashMap<>());
	}

	private void handleInvalidOpType(ConfigEntry configEntry) {
		throw new InvalidMigrationDefinitionException(configEntry.getType()
				+ " is not a valid config update type when creating a new topic. Only `SET` is accepted.");
	}

	private void validateOpTypes(MigrationDefinition migrationDefinition, List<OpType> acceptedTypes) {
		if (migrationDefinition.getConfigs() != null) {
			migrationDefinition.getConfigs().stream()

					.filter(configUpdate -> !acceptedTypes.contains(configUpdate.getType()))

					.findAny().ifPresent(this::handleInvalidOpType);
		}
	}

}
