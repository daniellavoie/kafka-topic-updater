package com.github.daniellavoie.topicnotary.core.kafka;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AccessControlEntryFilter;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.kafka.common.resource.ResourceType;

import com.github.daniellavoie.topicnotary.core.migration.AclEntry;

public class AclServiceImpl implements AclService {
	private final AdminClient adminClient;

	public AclServiceImpl(AdminClient adminClient) {
		this.adminClient = adminClient;
	}

	@Override
	public void createAcls(String topic, Set<AclEntry> acls) {
		try {
			adminClient
					.createAcls(
							acls.stream().map(aclEntry -> mapToBinding(topic, aclEntry)).collect(Collectors.toList()))
					.all().get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void deleteAcls(String topic, Set<AclEntry> acls) {
		try {
			adminClient.deleteAcls(
					acls.stream().map(aclEntry -> mapToBindingFilter(topic, aclEntry)).collect(Collectors.toList()))
					.all().get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	private AclBinding mapToBinding(String topic, AclEntry aclEntry) {
		return new AclBinding(new ResourcePattern(ResourceType.TOPIC, topic, aclEntry.getPatternType()),
				new AccessControlEntry(aclEntry.getPrincipal(), aclEntry.getHost(), aclEntry.getOperation(),
						aclEntry.getPermissionType()));
	}

	private AclBindingFilter mapToBindingFilter(String topic, AclEntry aclEntry) {
		return new AclBindingFilter(new ResourcePatternFilter(ResourceType.TOPIC, topic, aclEntry.getPatternType()),
				new AccessControlEntryFilter(aclEntry.getPrincipal(), aclEntry.getHost(), aclEntry.getOperation(),
						aclEntry.getPermissionType()));
	}

}
