package com.github.daniellavoie.topicnotary.core.kafka;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DeleteConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.GroupIdNotFoundException;

public class OffsetServiceImpl implements OffsetService {
	private final AdminClient adminClient;
	private final String groupId;

	public OffsetServiceImpl(AdminClient adminClient, String groupId) {
		this.adminClient = adminClient;
		this.groupId = groupId;
	}

	private Map<TopicPartition, OffsetSpec> buildOffsetsSpec(String topic, int partitions, OffsetSpec offsetSpec) {
		return IntStream.range(0, partitions).boxed()
				.collect(Collectors.toMap(partition -> new TopicPartition(topic, partition), info -> offsetSpec));
	}

	private void deleteConsumerGroupOffsetResult(Set<TopicPartition> topicPartitions)
			throws InterruptedException, ExecutionException {
		DeleteConsumerGroupOffsetsResult result = adminClient.deleteConsumerGroupOffsets(groupId, topicPartitions);

		try {
			result.all().get();
		} catch (ExecutionException ex) {
			if (ex.getCause() instanceof GroupIdNotFoundException) {
				return;
			}

			throw ex;
		}
	}

	@Override
	public Map<Integer, Long> findAndResetLatestOffset(String topic, int partitions) {
		try {
			// Identify the expected offsets to read.
			Map<TopicPartition, ListOffsetsResultInfo> listOffetsResult = adminClient
					.listOffsets(buildOffsetsSpec(topic, partitions, OffsetSpec.latest())).all().get();

			// Store the offsets to read.
			Map<Integer, Long> expectedOffsets = listOffetsResult.entrySet().stream()
					.collect(Collectors.toMap(entry -> entry.getKey().partition(), entry -> entry.getValue().offset()));

			// Reset the topic offset.
			deleteConsumerGroupOffsetResult(listOffetsResult.keySet().stream().collect(Collectors.toSet()));

			return expectedOffsets;
		} catch (InterruptedException | ExecutionException ex) {
			throw new RuntimeException(ex);
		}
	}
}
