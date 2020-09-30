package dev.daniellavoie.kafka.topic.updater.core.service;

import dev.daniellavoie.kafka.topic.updater.core.update.UpdateEntry;

public interface TopicService {
	void alterTopic(UpdateEntry updateEntry);

	void applyUpdate(UpdateEntry updateEntry);
	
	void createTopic(UpdateEntry updateEntry);
	
	void deleteTopic(UpdateEntry updateEntry);
}
