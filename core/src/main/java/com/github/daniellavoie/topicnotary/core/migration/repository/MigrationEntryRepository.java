package com.github.daniellavoie.topicnotary.core.migration.repository;

import java.util.List;

import com.github.daniellavoie.topicnotary.core.migration.MigrationEntry;

public interface MigrationEntryRepository {
	List<MigrationEntry> findAll();
	
	MigrationEntry save(MigrationEntry migrationEntry);
}
