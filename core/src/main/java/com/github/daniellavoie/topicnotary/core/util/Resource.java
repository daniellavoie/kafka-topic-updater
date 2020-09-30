package com.github.daniellavoie.topicnotary.core.util;

public class Resource {
	private final String filename;
	private final boolean inClasspath;

	public Resource(String filename, boolean inClasspath) {
		this.filename = filename;
		this.inClasspath = inClasspath;
	}

	public String getFilename() {
		return filename;
	}

	public boolean isInClasspath() {
		return inClasspath;
	}
}
