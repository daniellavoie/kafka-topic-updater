package com.github.daniellavoie.topicnotary.core.migration.exception;

public class InvalidMigrationDefinitionException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InvalidMigrationDefinitionException(Throwable cause) {
		super(cause);
	}

	public InvalidMigrationDefinitionException(String message) {
		super(message);
	}
}
