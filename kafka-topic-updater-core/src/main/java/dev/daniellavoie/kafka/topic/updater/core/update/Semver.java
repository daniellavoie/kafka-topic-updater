package dev.daniellavoie.kafka.topic.updater.core.update;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = Builder.class)
public class Semver {
	int major;
	int minor;
	int patch;
	String pre;

	@JsonPOJOBuilder(withPrefix = "")
	public static class Builder {
		// Lombok
	}
}
