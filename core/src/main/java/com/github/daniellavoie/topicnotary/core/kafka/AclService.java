package com.github.daniellavoie.topicnotary.core.kafka;

import java.util.Set;

import com.github.daniellavoie.topicnotary.core.migration.AclEntry;

public interface AclService {
	void createAcls(String topic, Set<AclEntry> acls);
	
	void deleteAcls(String topic, Set<AclEntry> acls);
}
