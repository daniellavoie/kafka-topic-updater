package dev.daniellavoie.kafka.topic.updater.core.repository;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaFuture;

import dev.daniellavoie.kafka.topic.updater.core.update.UpdateEntry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateEntryKafkaRepository implements UpdateEntryRepository {
	private final OffsetService offsetService;
	private final UpdatesTopic updatesTopic;
	private final AdminClient adminClient;
	private final Consumer<String, UpdateEntry> consumer;
	private final Producer<String, UpdateEntry> producer;
	private final TopicDescription topicDescription;

	public UpdateEntryKafkaRepository(OffsetService offsetService, UpdatesTopic updatesTopic,
			AdminClient adminClient, Map<String, Object> kafkaProperties) {
		this.offsetService = offsetService;
		this.updatesTopic = updatesTopic;
		this.adminClient = adminClient;
		this.consumer = new KafkaConsumer<>(kafkaProperties);
		this.producer = new KafkaProducer<>(kafkaProperties);
		this.topicDescription = initializeTopicIfNeeded();
	}

	private TopicDescription initializeTopic() {
		try {
			log.info("Creating topic {}.", updatesTopic.getName());

			NewTopic topic = new NewTopic(updatesTopic.getName(), updatesTopic.getPartitions(),
					updatesTopic.getReplicationFactor());

			topic.configs(updatesTopic.getConfigs());

			adminClient.createTopics(Arrays.asList(topic)).all().get();

			return topicDescription().orElseThrow(() -> new RuntimeException("Topic still missing after creation."));
		} catch (InterruptedException | ExecutionException ex) {
			throw new RuntimeException(ex);
		}
	}

	private <T> T get(KafkaFuture<T> future) {
		try {
			return future.get();
		} catch (InterruptedException | ExecutionException ex) {
			throw new RuntimeException(ex);
		}
	}

	private TopicDescription initializeTopicIfNeeded() {
		return Optional
				.ofNullable(adminClient.describeTopics(Arrays.asList(updatesTopic.getName())).values()
						.get(updatesTopic.getName()))

				.map(this::get).orElseGet(this::initializeTopic);
	}

	private Optional<TopicDescription> topicDescription() {
		DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(Arrays.asList(updatesTopic.getName()));

		KafkaFuture<TopicDescription> futureTopicDescription = describeTopicsResult.values()
				.get(updatesTopic.getName());

		return Optional.ofNullable(futureTopicDescription).map(this::get);
	}

	private List<UpdateEntry> loadExistingUpdateEntries(Map<Integer, Long> expectedOffsets) {
		List<UpdateEntry> entries = new LinkedList<>();
		Set<Integer> fullyReadedPartitions = new HashSet<>();

		boolean completed = false;
		boolean lastPollEmpty = false;

		consumer.subscribe(Arrays.asList(updatesTopic.getName()));

		do {
			ConsumerRecords<String, UpdateEntry> records = consumer.poll(Duration.ofSeconds(2));
			Iterator<ConsumerRecord<String, UpdateEntry>> iterator = records.iterator();

			while (iterator.hasNext()) {
				ConsumerRecord<String, UpdateEntry> record = iterator.next();

				entries.add(record.value());

				if (expectedOffsets.get(record.partition()) <= record.offset()) {
					fullyReadedPartitions.add(record.partition());
				}
			}

			lastPollEmpty = records.isEmpty();
			completed = fullyReadedPartitions.size() == expectedOffsets.size();
		} while (!completed && !lastPollEmpty);

		return entries;
	}

	@Override
	public synchronized List<UpdateEntry> findAll() {
		return loadExistingUpdateEntries(findExpectedOffets());
	}

	private Map<Integer, Long> findExpectedOffets() {
		return offsetService.findAndResetLatestOffset(topicDescription.name(), topicDescription.partitions().size());
	}

	@Override
	public UpdateEntry save(UpdateEntry updateEntry) {
		producer.beginTransaction();
		try {

			producer.send(new ProducerRecord<String, UpdateEntry>(updatesTopic.getName(),
					updateEntry.getUpdateDefinition().getTopic(), updateEntry)).get();

			producer.commitTransaction();

			return updateEntry;
		} catch (InterruptedException | ExecutionException ex) {
			producer.abortTransaction();

			throw new RuntimeException(ex);
		}
	}
}
