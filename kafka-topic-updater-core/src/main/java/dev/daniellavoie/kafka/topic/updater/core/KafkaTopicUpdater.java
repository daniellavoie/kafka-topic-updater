package dev.daniellavoie.kafka.topic.updater.core;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.TopicDescription;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import dev.daniellavoie.kafka.topic.updater.core.repository.UpdateEntryRepository;
import dev.daniellavoie.kafka.topic.updater.core.update.UpdateDefinitionUtil;
import dev.daniellavoie.kafka.topic.updater.core.update.UpdateEntry;

public class KafkaTopicUpdater {
	private final UpdateEntryRepository updateEntryRepository;
	private final Configuration configuration;
	
	public KafkaTopicUpdater(UpdateEntryRepository updateEntryRepository, Configuration configuration) {
		this.updateEntryRepository = updateEntryRepository;
		this.configuration = configuration;
	}

	public void run() throws InterruptedException, ExecutionException {
		List<UpdateEntry> existingUpdateEntries = updateEntryRepository.findAll();
		List<UpdateEntry> updateEntries = loadUpdateEntries();
		
		// Assert that all old updates matches with version and checksum. 
		compareExistingUpdates(existingUpdateEntries, updateEntries);
		
		// 
	}
	
	private void alterTopic(UpdateEntry updateEntry) {
		
	}

	private void applyUpdate(UpdateEntry updateEntry) {
		
	}
	
	private void createTopic(UpdateEntry updateEntry) {
		
	}
	
	private void deleteTopic(UpdateEntry updateEntry) {
		
	}

	private void compareExistingUpdates(List<UpdateEntry> existingUpdateEntries, List<UpdateEntry> updateEntries) {
		IntStream.range(0, existingUpdateEntries.size())

				.forEach(i -> compareUpdateEntries(existingUpdateEntries.get(i), Optional.of(updateEntries)
						.filter(entries -> entries.size() > i).map(entries -> entries.get(i))));
	}

	private ChecksumMismatchException buildVersionNotFoundException(String version) {
		return new ChecksumMismatchException("previous update definition for " + version + " could not found");
	}

	private void compareUpdateEntries(UpdateEntry existingUpdateEntry, Optional<UpdateEntry> optionalUpdateEntry) {
		UpdateEntry updateEntry = optionalUpdateEntry
				.orElseThrow(() -> buildVersionNotFoundException(existingUpdateEntry.getVersion()));

		if (!updateEntry.getVersion().equals(updateEntry.getVersion())) {
			throw buildVersionNotFoundException(existingUpdateEntry.getVersion());
		}

		if (!updateEntry.getChecksum().equals(existingUpdateEntry.getChecksum())) {
			throw new ChecksumMismatchException("upgrade " + updateEntry.getVersion()
					+ " has already been completed with checksum " + existingUpdateEntry.getChecksum()
					+ " and expected checksum is " + updateEntry.getChecksum());
		}
	}



	private List<UpdateEntry> loadUpdateEntries() {
		try {
			return Arrays.stream(new PathMatchingResourcePatternResolver().getResources(configuration.getUpdatesDir()))
					.map(UpdateDefinitionUtil::buildUpdateEntry).collect(Collectors.toList());
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
