package com.github.daniellavoie.topicnotary.core;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.github.daniellavoie.topicnotary.core.kafka.AclService;
import com.github.daniellavoie.topicnotary.core.kafka.AclServiceImpl;
import com.github.daniellavoie.topicnotary.core.kafka.OffsetService;
import com.github.daniellavoie.topicnotary.core.kafka.OffsetServiceImpl;
import com.github.daniellavoie.topicnotary.core.kafka.TopicService;
import com.github.daniellavoie.topicnotary.core.kafka.TopicServiceImpl;
import com.github.daniellavoie.topicnotary.core.migration.MigrationService;
import com.github.daniellavoie.topicnotary.core.migration.MigrationServiceImpl;
import com.github.daniellavoie.topicnotary.core.migration.MigrationTopic;
import com.github.daniellavoie.topicnotary.core.migration.repository.MigrationEntryKafkaRepository;

@SpringBootApplication
public class CoreTestApplication {
	@Bean
	public AclService aclService(AdminClient adminClient) {
		return new AclServiceImpl(adminClient);
	}

	@Bean
	public AdminClient adminClient(KafkaProperties kafkaProperties) {
		return AdminClient.create(kafkaProperties.buildAdminProperties());
	}

	@Bean
	@ConfigurationProperties
	public Configuration configuration() {
		return new Configuration();
	}

	@Bean
	@ConfigurationProperties("migration-topic")
	public MigrationTopic migrationTopic() {
		return new MigrationTopic();
	}

	@Bean
	public MigrationEntryKafkaRepository migrationEntryKafkaRepository(OffsetService offsetService,
			MigrationTopic migrationTopic, AdminClient adminClient, KafkaProperties kafkaProperties) {
		return new MigrationEntryKafkaRepository(offsetService, migrationTopic, adminClient,
				kafkaProperties.buildConsumerProperties(), kafkaProperties.buildProducerProperties());
	}

	@Bean
	public MigrationService migrationService(AclService aclService, Configuration configuration,
			MigrationEntryKafkaRepository repository, TopicService topicService) {
		return new MigrationServiceImpl(aclService, configuration, repository, topicService);
	}

	@Bean
	public OffsetService offsetService(AdminClient adminClient, KafkaProperties kafkaProperties) {
		return new OffsetServiceImpl(adminClient, kafkaProperties.getConsumer().getGroupId());
	}

	@Bean
	public TopicService topicService(AdminClient adminClient) {
		return new TopicServiceImpl(adminClient);
	}
}
