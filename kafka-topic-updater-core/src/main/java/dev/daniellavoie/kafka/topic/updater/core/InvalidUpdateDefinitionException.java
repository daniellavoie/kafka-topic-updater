package dev.daniellavoie.kafka.topic.updater.core;

public class InvalidUpdateDefinitionException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InvalidUpdateDefinitionException(Throwable cause) {
		super(cause);
	}

	public InvalidUpdateDefinitionException(String message) {
		super(message);
	}
}
