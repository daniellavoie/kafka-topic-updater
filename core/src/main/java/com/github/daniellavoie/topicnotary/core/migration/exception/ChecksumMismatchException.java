package com.github.daniellavoie.topicnotary.core.migration.exception;

public class ChecksumMismatchException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ChecksumMismatchException(String message) {
		super(message);
	}
}
