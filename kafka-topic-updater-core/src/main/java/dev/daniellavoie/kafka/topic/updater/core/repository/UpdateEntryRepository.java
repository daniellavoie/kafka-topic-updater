package dev.daniellavoie.kafka.topic.updater.core.repository;

import java.util.List;

import dev.daniellavoie.kafka.topic.updater.core.update.UpdateEntry;

public interface UpdateEntryRepository {
	List<UpdateEntry> findAll();
	
	UpdateEntry save(UpdateEntry updateEntry);
}
