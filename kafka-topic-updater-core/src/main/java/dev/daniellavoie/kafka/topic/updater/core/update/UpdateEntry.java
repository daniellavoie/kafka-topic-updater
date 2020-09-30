package dev.daniellavoie.kafka.topic.updater.core.update;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = Builder.class)
public class UpdateEntry {
	String checksum;
	String version;
	String description;
	LocalDateTime timestamp;

	UpdateDefinition updateDefinition;
	
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        // Lombok
    }
}
