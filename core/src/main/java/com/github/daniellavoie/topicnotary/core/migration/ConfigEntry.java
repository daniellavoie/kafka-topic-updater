package com.github.daniellavoie.topicnotary.core.migration;

import org.apache.kafka.clients.admin.AlterConfigOp.OpType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ConfigEntry {
	private final String config;
	private final OpType type;
	private final String value;
	
	@JsonCreator
	public ConfigEntry(@JsonProperty("config") String config, @JsonProperty("type") OpType type, @JsonProperty("value") String value) {
		this.config = config;
		this.type = type;
		this.value = value;
	}

	public String getConfig() {
		return config;
	}

	public OpType getType() {
		return type;
	}

	public String getValue() {
		return value;
	}
}
