package com.github.daniellavoie.topicnotary.core.migration.repository;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.daniellavoie.topicnotary.core.kafka.OffsetService;
import com.github.daniellavoie.topicnotary.core.migration.MigrationEntry;
import com.github.daniellavoie.topicnotary.core.migration.MigrationTopic;

public class MigrationEntryKafkaRepository implements MigrationEntryRepository {
	private final Logger LOGGER = LoggerFactory.getLogger(MigrationEntryKafkaRepository.class);

	private final OffsetService offsetService;
	private final MigrationTopic migrationTopic;
	private final AdminClient adminClient;
	private final Map<String, Object> consumerProperties;
	private final JsonSerdes<MigrationEntry> updateEntrySerdes;
	private final Producer<String, MigrationEntry> producer;

	private final TopicDescription topicDescription;

	public MigrationEntryKafkaRepository(OffsetService offsetService, MigrationTopic migrationTopic,
			AdminClient adminClient, Map<String, Object> consumerProperties, Map<String, Object> producerProperties) {
		this.offsetService = offsetService;
		this.migrationTopic = migrationTopic;
		this.adminClient = adminClient;
		this.consumerProperties = buildConsumerProperties(consumerProperties);
		this.updateEntrySerdes = new JsonSerdes<>(MigrationEntry.class);

		this.producer = new KafkaProducer<>(buildProducerProperties(producerProperties), new StringSerializer(),
				updateEntrySerdes);

		this.topicDescription = initializeTopicIfNeeded();
	}

	private Map<String, Object> buildConsumerProperties(Map<String, Object> baseConsumerProperties) {
		Map<String, Object> consumerProperties = new HashMap<>(baseConsumerProperties);

		consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

		return consumerProperties;
	}

	private Map<String, Object> buildProducerProperties(Map<String, Object> baseProducerProperties) {
		Map<String, Object> consumerProperties = new HashMap<>(baseProducerProperties);

		consumerProperties.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, UUID.randomUUID().toString());

		return consumerProperties;
	}

	private TopicDescription initializeTopic() {
		int maxAttempt = 3;
		int attempt = 0;

		do {
			try {
				LOGGER.info("Creating topic {}.", migrationTopic.getName());

				NewTopic topic = new NewTopic(migrationTopic.getName(), migrationTopic.getPartitions(),
						migrationTopic.getReplicationFactor());

				topic.configs(migrationTopic.getConfigs());

				adminClient.createTopics(Arrays.asList(topic)).all().get();

				return topicDescription()
						.orElseThrow(() -> new RuntimeException("Topic still missing after creation."));
			} catch (InterruptedException | ExecutionException ex) {
				if (ex.getCause() != null && ex.getCause() instanceof TopicExistsException && attempt <= maxAttempt) {
					LOGGER.warn("Failed to create the migration topic. Retrying in 5 seconds.");
					++attempt;
					
					try {
						Thread.sleep(5000);
					} catch (InterruptedException interruptedEx) {
						throw new RuntimeException(interruptedEx);
					}
				} else {
					throw new RuntimeException(ex);
				}
			}
		} while (true);
	}

	private <T> Optional<T> get(KafkaFuture<T> future) {
		try {
			return Optional.of(future.get());
		} catch (InterruptedException | ExecutionException ex) {
			if (ex.getCause() instanceof UnknownTopicOrPartitionException) {
				return Optional.empty();
			}

			throw new RuntimeException(ex);
		}
	}

	public TopicDescription initializeTopicIfNeeded() {
		return Optional
				.ofNullable(adminClient.describeTopics(Arrays.asList(migrationTopic.getName())).values()
						.get(migrationTopic.getName()))

				.flatMap(this::get).orElseGet(this::initializeTopic);
	}

	private Optional<TopicDescription> topicDescription() {
		DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(Arrays.asList(migrationTopic.getName()));

		KafkaFuture<TopicDescription> futureTopicDescription = describeTopicsResult.values()
				.get(migrationTopic.getName());

		return Optional.ofNullable(futureTopicDescription).flatMap(this::get);
	}

	private List<MigrationEntry> loadExistingUpdateEntries(Map<Integer, Long> expectedOffsets) {
		List<MigrationEntry> entries = new LinkedList<>();
		Set<Integer> fullyReadedPartitions = new HashSet<>();

		boolean completed = false;
		boolean lastPollEmpty = false;

		KafkaConsumer<String, MigrationEntry> consumer = new KafkaConsumer<>(consumerProperties,
				new StringDeserializer(), updateEntrySerdes);
		try {
			consumer.subscribe(Arrays.asList(migrationTopic.getName()));
			do {
				ConsumerRecords<String, MigrationEntry> records = consumer.poll(Duration.ofSeconds(5));
				Iterator<ConsumerRecord<String, MigrationEntry>> iterator = records.iterator();

				while (iterator.hasNext()) {
					ConsumerRecord<String, MigrationEntry> record = iterator.next();

					entries.add(record.value());

					if (expectedOffsets.get(record.partition()) <= record.offset()) {
						fullyReadedPartitions.add(record.partition());
					}
				}

				lastPollEmpty = records.isEmpty();
				completed = fullyReadedPartitions.size() == expectedOffsets.size();
			} while (!completed && !lastPollEmpty);
		} finally {
			consumer.close();
		}

		return entries;
	}

	@Override
	public synchronized List<MigrationEntry> findAll() {
		return loadExistingUpdateEntries(findExpectedOffets());
	}

	private Map<Integer, Long> findExpectedOffets() {
		return offsetService.findAndResetLatestOffset(topicDescription.name(), topicDescription.partitions().size());
	}

	@Override
	public MigrationEntry save(MigrationEntry migrationEntry) {
		try {
			producer.initTransactions();

		} catch (KafkaException kafkaEx) {
			if (!kafkaEx.getMessage().endsWith("Invalid transition attempted from state READY to state INITIALIZING")) {
				throw new RuntimeException(kafkaEx);
			}
		}
		producer.beginTransaction();

		try {
			producer.send(new ProducerRecord<String, MigrationEntry>(migrationTopic.getName(),
					migrationEntry.getEnvironment(), migrationEntry)).get();

			producer.commitTransaction();

			return migrationEntry;
		} catch (InterruptedException | ExecutionException ex) {
			producer.abortTransaction();

			throw new RuntimeException(ex);
		}
	}
}
