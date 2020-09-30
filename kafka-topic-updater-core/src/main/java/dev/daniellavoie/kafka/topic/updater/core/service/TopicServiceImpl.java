package dev.daniellavoie.kafka.topic.updater.core.service;

import org.apache.kafka.clients.admin.AdminClient;

import dev.daniellavoie.kafka.topic.updater.core.update.UpdateEntry;

public class TopicServiceImpl implements TopicService {
	private final AdminClient adminClient;

	public TopicServiceImpl(AdminClient adminClient) {
		this.adminClient = adminClient;
	}

	@Override
	public void alterTopic(UpdateEntry updateEntry) {
//		ConfigEntry configEntry = new ConfigEntry(name, value);
//		
//		AlterConfigOp op = new AlterConfigOp(configEntry, operationType)
//		
//		adminClient.incrementalAlterConfigs(configs)
	}

//	private List<AlterConfigOp> configEntry(UpdateEntry updateEntry) {
//		return updateEntry.getUpdateDefinition().getConfigs().entrySet().stream()
//				.map(entry -> new AlterConfigOp(new ConfigEntry(entry.getKey(), entry.getValue()), OpType.).collect(Collectors.toList());
//	}

	@Override
	public void applyUpdate(UpdateEntry updateEntry) {
		// TODO Auto-generated method stub

	}

	@Override
	public void createTopic(UpdateEntry updateEntry) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteTopic(UpdateEntry updateEntry) {
		// TODO Auto-generated method stub

	}

}
