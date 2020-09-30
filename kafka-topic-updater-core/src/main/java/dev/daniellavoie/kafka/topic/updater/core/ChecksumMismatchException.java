package dev.daniellavoie.kafka.topic.updater.core;

public class ChecksumMismatchException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ChecksumMismatchException(String message) {
		super(message);
	}
}
