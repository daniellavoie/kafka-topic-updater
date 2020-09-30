package com.github.daniellavoie.topicnotary.core.kafka;

import com.github.daniellavoie.topicnotary.core.migration.MigrationDefinition;

public interface TopicService {
	void alterTopic(MigrationDefinition migrationDefinition);
	
	void createTopic(MigrationDefinition migrationDefinition);
	
	void deleteTopic(MigrationDefinition migrationDefinition);
}
