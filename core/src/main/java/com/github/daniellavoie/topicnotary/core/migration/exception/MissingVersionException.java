package com.github.daniellavoie.topicnotary.core.migration.exception;

public class MissingVersionException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public MissingVersionException(String message) {
		super(message);
	}
}
