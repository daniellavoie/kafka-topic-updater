package dev.daniellavoie.kafka.topic.updater.core;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import dev.daniellavoie.kafka.topic.updater.core.repository.UpdatesTopic;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Value
@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = Builder.class)
public class Configuration {
	private UpdatesTopic updatesTopic;
	private String updatesDir;
	
	
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        // Lombok
    }
}
