package com.github.daniellavoie.topicnotary.cli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.github.daniellavoie.topicnotary.core.Configuration;
import com.github.daniellavoie.topicnotary.core.migration.MigrationTopic;

@SpringBootApplication
public class TopicNotaryCli implements CommandLineRunner {
	public static void main(String[] args) {
		SpringApplication.run(TopicNotaryCli.class, args);
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

	@Override
	public void run(String... args) throws Exception {
		// Migration is done by the initializer bean.
	}
}
