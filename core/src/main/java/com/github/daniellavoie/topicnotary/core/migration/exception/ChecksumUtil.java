package com.github.daniellavoie.topicnotary.core.migration.exception;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.daniellavoie.topicnotary.core.migration.MigrationEntry;

public abstract class ChecksumUtil {

	private static MissingVersionException buildVersionNotFoundException(String version) {
		return new MissingVersionException("previous update definition for " + version + " could not found");
	}

	public static List<MigrationEntry> compareExistingUpdates(List<MigrationEntry> existingUpdateEntries,
			List<MigrationEntry> migrationEntries) {
		IntStream.range(0, existingUpdateEntries.size())

				.forEach(i -> compareUpdateEntries(existingUpdateEntries.get(i), Optional.of(migrationEntries)
						.filter(entries -> entries.size() > i).map(entries -> entries.get(i))));
		
		return IntStream.range(existingUpdateEntries.size(), migrationEntries.size()).boxed().map(index->migrationEntries.get(index)).collect(Collectors.toList());
	}

	private static void compareUpdateEntries(MigrationEntry existingUpdateEntry,
			Optional<MigrationEntry> optionalUpdateEntry) {
		MigrationEntry migrationEntry = optionalUpdateEntry
				.orElseThrow(() -> buildVersionNotFoundException(existingUpdateEntry.getVersion()));

		if (!migrationEntry.getVersion().equals(migrationEntry.getVersion())) {
			throw buildVersionNotFoundException(existingUpdateEntry.getVersion());
		}

		if (!migrationEntry.getChecksum().equals(existingUpdateEntry.getChecksum())) {
			throw new ChecksumMismatchException("upgrade " + migrationEntry.getVersion()
					+ " has already been completed with checksum " + existingUpdateEntry.getChecksum()
					+ " and expected checksum is " + migrationEntry.getChecksum());
		}
	}
}
