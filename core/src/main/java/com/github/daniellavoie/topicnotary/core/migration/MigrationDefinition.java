package com.github.daniellavoie.topicnotary.core.migration;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MigrationDefinition {
	public enum UpdateType {
		CREATE, ALTER, DELETE
	}

	private final String topic;
	private final UpdateType updateType;
	private final Integer partitions;
	private final Short replicationFactor;
	private final Set<ConfigEntry> configs;
	private final Set<AclEntry> createAcls;
	private final Set<AclEntry> deleteAcls;

	@JsonCreator
	public MigrationDefinition(@JsonProperty("topic") String topic, @JsonProperty("updateType") UpdateType updateType,
			@JsonProperty("partitions") Integer partitions, @JsonProperty("replicationFactor") Short replicationFactor,
			@JsonProperty("configs") Set<ConfigEntry> configs, @JsonProperty("createAcls") Set<AclEntry> createAcls,
			@JsonProperty("deleteAcls") Set<AclEntry> deleteAcls) {
		this.topic = topic;
		this.updateType = updateType;
		this.partitions = partitions;
		this.replicationFactor = replicationFactor;
		this.configs = configs;
		this.createAcls = createAcls;
		this.deleteAcls = deleteAcls;
	}

	public String getTopic() {
		return topic;
	}

	public UpdateType getUpdateType() {
		return updateType;
	}

	public Integer getPartitions() {
		return partitions;
	}

	public Short getReplicationFactor() {
		return replicationFactor;
	}

	public Set<ConfigEntry> getConfigs() {
		return configs;
	}

	public Set<AclEntry> getCreateAcls() {
		return createAcls;
	}

	public Set<AclEntry> getDeleteAcls() {
		return deleteAcls;
	}
}
