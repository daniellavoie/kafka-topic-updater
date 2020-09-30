package com.github.daniellavoie.topicnotary.core;

public class Configuration {
	private String environmentName;
	private String migrationsDir = "classpath:/kafka/migration";

	public String getEnvironmentName() {
		return environmentName;
	}

	public void setEnvironmentName(String environmentName) {
		this.environmentName = environmentName;
	}

	public String getMigrationsDir() {
		return migrationsDir;
	}

	public void setMigrationsDir(String migrationsDir) {
		this.migrationsDir = migrationsDir;
	}
}
