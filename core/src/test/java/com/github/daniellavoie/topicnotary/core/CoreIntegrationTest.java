package com.github.daniellavoie.topicnotary.core;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = CoreTestApplication.class)
public class CoreIntegrationTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(CoreIntegrationTest.class);

	@Autowired
	protected AdminClient adminClient;

	protected void cleanupTopic(String... topics) throws InterruptedException, ExecutionException {
		try {
			adminClient.deleteTopics(Arrays.stream(topics).collect(Collectors.toList())).all().get();
		} catch (ExecutionException ex) {
			if (ex.getCause() instanceof UnknownTopicOrPartitionException) {
				LOGGER.info("Skipped topic cleanup since it does not exist");
			} else {
				throw ex;
			}
		}
	}
}
