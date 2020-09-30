package dev.daniellavoie.kafka.topic.updater.core.repository;

import java.util.Map;

public interface OffsetService {
	Map<Integer, Long> findAndResetLatestOffset(String topic, int partitions);
}
