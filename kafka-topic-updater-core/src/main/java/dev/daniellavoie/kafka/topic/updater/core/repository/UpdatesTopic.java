package dev.daniellavoie.kafka.topic.updater.core.repository;

import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = Builder.class)
public class UpdatesTopic {
	boolean autoCreate = true;
	String name;
	int partitions;
	short replicationFactor; 
	Map<String, String> configs;
	
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        // Lombok
    }
}
