package com.github.daniellavoie.topicnotary.core.kafka;

import java.util.Map;

public interface OffsetService {
	Map<Integer, Long> findAndResetLatestOffset(String topic, int partitions);
}
