package com.github.daniellavoie.topicnotary.spring;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;

import com.github.daniellavoie.topicnotary.core.migration.MigrationService;

public class TopicNotaryInitializer implements InitializingBean, Ordered {
	private final MigrationService migrationService;

	private int order = 0;

	public TopicNotaryInitializer(MigrationService migrationService) {
		this.migrationService = migrationService;
	}

	@Override
	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		migrationService.migrate();
	}

}
