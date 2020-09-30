package com.github.daniellavoie.topicnotary.core.migration;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MigrationEntry {
	private final String environment;
	private final String checksum;
	private final String version;
	private final String description;
	private final LocalDateTime timestamp;
	private final List<MigrationDefinition> definitions;

	@JsonCreator
	public MigrationEntry(@JsonProperty("environment") String environment, @JsonProperty("checksum") String checksum,
			@JsonProperty("version") String version, @JsonProperty("description") String description,
			@JsonProperty("timestamp") LocalDateTime timestamp,
			@JsonProperty("definitions") List<MigrationDefinition> definitions) {
		this.environment = environment;
		this.checksum = checksum;
		this.version = version;
		this.description = description;
		this.timestamp = timestamp;
		this.definitions = definitions;
	}

	public String getEnvironment() {
		return environment;
	}

	public String getChecksum() {
		return checksum;
	}

	public String getVersion() {
		return version;
	}

	public String getDescription() {
		return description;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public List<MigrationDefinition> getDefinitions() {
		return definitions;
	}
}
