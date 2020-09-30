package com.github.daniellavoie.topicnotary.core.migration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.daniellavoie.topicnotary.core.migration.exception.ChecksumMismatchException;
import com.github.daniellavoie.topicnotary.core.migration.exception.ChecksumUtil;
import com.github.daniellavoie.topicnotary.core.migration.exception.MissingVersionException;

public class ChecksumUtilTest {
	@Test
	public void checksumMatches() {
		List<MigrationEntry> entries = MigrationEntryUtil.loadMigrationEntries("unit-test", "classpath:kafka/migration");

		Assertions.assertNotNull(entries);

		ChecksumUtil.compareExistingUpdates(entries, entries);
	}

	@Test
	public void checksumDoesNotMatch() {
		List<MigrationEntry> entries = MigrationEntryUtil.loadMigrationEntries("unit-test", "classpath:kafka/migration");

		Assertions.assertNotNull(entries);

		MigrationEntry lastEntry = entries.get(entries.size() - 1);

		MigrationEntry mutatedEnty = new MigrationEntry(lastEntry.getEnvironment(), "11111111111111111111",
				lastEntry.getVersion(), lastEntry.getDescription(), LocalDateTime.now(), lastEntry.getDefinitions());

		List<MigrationEntry> mutatedList = new ArrayList<>(entries);
		mutatedList.remove(entries.size() - 1);
		mutatedList.add(mutatedEnty);

		Assertions.assertThrows(ChecksumMismatchException.class,
				() -> ChecksumUtil.compareExistingUpdates(entries, mutatedList));
	}

	@Test
	public void missingVersion() {
		List<MigrationEntry> entries = MigrationEntryUtil.loadMigrationEntries("unit-test", "classpath:kafka/migration");

		Assertions.assertNotNull(entries);

		List<MigrationEntry> mutatedList = new ArrayList<>(entries);
		mutatedList.remove(entries.size() - 1);

		Assertions.assertThrows(MissingVersionException.class,
				() -> ChecksumUtil.compareExistingUpdates(entries, mutatedList));
	}
}
