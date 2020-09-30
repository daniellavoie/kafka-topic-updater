package com.github.daniellavoie.topicnotary.core.migration.repository;

import java.io.IOException;
import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonSerdes<T> implements Serializer<T>, Deserializer<T> {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

	private final Class<T> type;

	public JsonSerdes(Class<T> type) {
		this.type = type;
	}

	@Override
	public T deserialize(String topic, byte[] data) {
		try {
			return OBJECT_MAPPER.readValue(data, type);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public byte[] serialize(String topic, T data) {
		try {
			return OBJECT_MAPPER.writeValueAsBytes(data);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void configure(Map<String, ?> configs, boolean isKey) {
		// TODO Auto-generated method stub
		Deserializer.super.configure(configs, isKey);
	}

	@Override
	public void close() {
		// Do nothing
	}
}
