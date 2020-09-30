package com.github.daniellavoie.topicnotary.core.migration;

import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AclEntry {
	private final String principal;
	private final String host;
	private final AclOperation operation;
	private final AclPermissionType permissionType;
	private final PatternType patternType;
	
	@JsonCreator
	public AclEntry(
			@JsonProperty("principal") String principal, 
			@JsonProperty("host") String host, 
			@JsonProperty("operation") AclOperation operation, 
			@JsonProperty("permissionType") AclPermissionType permissionType,
			@JsonProperty("patternType") PatternType patternType) {
		this.principal = principal;
		this.host = host;
		this.operation = operation;
		this.permissionType = permissionType;
		this.patternType = patternType;
	}

	public String getPrincipal() {
		return principal;
	}

	public String getHost() {
		return host;
	}

	public AclOperation getOperation() {
		return operation;
	}

	public AclPermissionType getPermissionType() {
		return permissionType;
	}

	public PatternType getPatternType() {
		return patternType;
	}
}
