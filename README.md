# Kafka Topic Updater

## Motivation

This project is inspired by the awesome [Flywaydb](https://flywaydb.org/) project. Topic, Schema and ACL management in Kafka are an imperative task. One cannot point Kafka to git and hope to manage such resources declaratively.

This projects aims to bring declarative resource management to Kafka while still keeping safeguard for production deployments.

This tool can be used within a CI/CD pipeline or embedded in your Kafka application for automatic resource management.

## Why versioned incremental upgrades? Why not just pure declarative?

There's a lot of appeal at looking at your git repository for configuration and observe a full snapshot of what you should be applied to the cluster. You can basically take the whole config and apply it on a fresh cluster to recreate the environment. There's almost a  WYSIWYG feel to it. 

However, it is a double edge sword. The configurations and state of a Kafka topic is very sensitive as it is generally used as a contract between teams and application to exchange information. For systems that are running in production, worst thing you can possible do run an unexpected manifest in production have the entire state be overwritten without any control. That is the same reason why nobody will run Hibernate on automatic schema management in production. Behavior needs to be predictable

This projects implements the best possible tradeoff by validating and applying incremental changes that are versioned as part of your configuration repository. For each change to a topic, acl or schema, the tool will persist a checksum and a semver for each upgrade defined in your configuration repository. 

For example, your configuration repository would contains 3 manifests; `V1_0_0__Initial_Release.json`, `V1_0_1__Hot_Fix_1232141.json`, `V1_1_0__Minor_Release.json`. The tool will identify which of these manifests were already executed on your target cluster, ensure the checksum for these match and apply the delta of the upgrades that follows the baseline version of the cluster.

This system allows to take those configurations and point at any cluster and recreate the desired state through the sequential upgrade definitions.

## Features

* Incremental and versioned updates of kafka topics [In Progress]
* Checksum validation from previously executed upgrades versus the one available in the configuration repository [In Progress]
* Spring Boot support with dedicated ` spring-boot-starter-kafka-topic-updater` [TODO]
* Runnable CLI support with `kafka-topic-updater.jar` [TODO]

## Future features

* ACL upgrade support [TODO]
* Schema Registry and schemas upgrade support [TODO]
* Kubernetes CRD [TODO]

## Current state

The project is still a work and progress and does not work as is. The source is code is opened for everyone to observice