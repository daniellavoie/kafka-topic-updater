package dev.daniellavoie.kafka.topic.updater.core.update;

import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = Builder.class)
public class UpdateDefinition {
	public enum UpdateType {
		CREATE, ALTER, DELETE
	}
	
	String topic;
	UpdateType updateType;
	int partitions;
	short replicationFactor;
	Map<String, String> configs;
	
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        // Lombok
    }
}
