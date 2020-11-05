# Kafka Topic Updater

## Overview

### Motivation

This project is inspired by the awesome [Flywaydb](https://flywaydb.org/) project. Topic, Schema and ACL management in Kafka are an imperative task. One cannot point Kafka to git and hope to manage such resources declaratively.

This projects aims to bring declarative resource management to Kafka while still keeping safeguard for production deployments.

This tool can be used within a CI/CD pipeline or embedded in your Kafka application for automatic resource management.

### Why versioned incremental upgrades? Why not just pure declarative?

There's a lot of appeal at looking at your git repository for configuration and observe a full snapshot of what you should be applied to the cluster. You can basically take the whole config and apply it on a fresh cluster to recreate the environment. There's almost a  WYSIWYG feel to it. 

However, it is a double edge sword. The configurations and state of a Kafka topic is very sensitive as it is generally used as a contract between teams and application to exchange information. For systems that are running in production, worst thing you can possible do run an unexpected manifest in production have the entire state be overwritten without any control. That is the same reason why nobody will run Hibernate on automatic schema management in production. Behavior needs to be predictable

This projects implements the best possible tradeoff by validating and applying incremental changes that are versioned as part of your configuration repository. For each change to a topic, acl or schema, the tool will persist a checksum and a semver for each upgrade defined in your configuration repository. 

For example, your configuration repository would contains 3 manifests; `V1_0_0__Initial_Release.json`, `V1_0_1__Hot_Fix_1232141.json`, `V1_1_0__Minor_Release.json`. The tool will identify which of these manifests were already executed on your target cluster, ensure the checksum for these match and apply the delta of the upgrades that follows the baseline version of the cluster.

This system allows to take those configurations and point at any cluster and recreate the desired state through the sequential upgrade definitions.

### Features

* Incremental and versioned updates of kafka topics
* Checksum validation from previously executed upgrades versus the one available in the configuration repository
* Runnable CLI support with `kafka-topic-updater.jar`
* Spring Boot support with dedicated ` spring-boot-starter-kafka-topic-updater`
* Topic ACL support

### Upcoming features

* Schema Registry and schemas upgrade support

## Getting started

## Pre-requisite

* Java > 1.8
* Access to a Kafka bootstrap server (one is provided with docker-compose through the (run-kafka.sh)[run-kafka.sh] script.
* Administrative permissions if the ACL are enabled on the Kafka cluster (pre-configured with provided docker-compose).

### Build

This project is packaged with the (Maven Wrapper)[https://github.com/takari/maven-wrapper]. It can be build  using `./mvw` or `mvn.bat` commands.

```
./mvnw clean package
```

### Anatomy of a migration manifest

First manifest : `V1_0_0__Initial_Migration.json`

```
[
  {
    "topic": "topic-with-configs",  // Topic name
    "updateType": "CREATE",         // CREATE, ALTER or DELETE
    "partitions": 5,                // Partition count
    "replicationFactor": 1,         // Replication factor
    "configs": [                    // Topic Configs
      {
        "config": "cleanup.policy", // Config name
        "type": "SET",              // SET, DELETE, APPEND or SUBTRACT
        "value": "delete"           // Config value
      },
      {
        "config": "retention.ms",
        "type": "SET",
        "value": "-1"
      }
    ],
    "createAcls": [                 // Array of standard Kafka ACL definition
      {
        "principal": "User:app",
        "host": "*",
        "operation": "READ",
        "permissionType":"ALLOW",
        "patternType":"LITERAL"
      }
    ]
  }
[
```

Patch release manifest : `V1_0_1__Patch_Release.json`

```
[
  {
    "topic": "topic-with-configs",
    "updateType": "ALTER",
    "deleteAcls": [                 // Array of standard Kafka ACL definition
      {
        "principal": "User:app",
        "host": "*",
        "operation": "READ",
        "permissionType":"ALLOW",
        "patternType":"LITERAL"
      }
    ]
  }
]
```

Major release manifest : `V2_0_0__Delete_Topic_Showcase.json`

```
[
  {
    "topic": "topic-with-configs",
    "updateType": "DELETE"
  }
]
```


### Running locally with Docker Compose

A convenient (docker-compose/docker-compose.yml)[docker-compose file] is included in the project that allows to setup and run all the infrastructure required to get started with local test or even local development if you wish to contribute to the project. The Kafka cluster is pre configured with SASL Plain and an ACL Authorizer effectively allowing management of ACL create and delete within migration manifests. The `./run-kafka.sh` will setup the docker-compose deployment for you. 

```
./run-kafka.sh && \
  ./cli/target/topic-notary-cli.jar \
    --environment-name=my-env \
    --migrations-dir=../../doc/sample-migrations \
    --sasl.mechanism=PLAIN \
    --security.protocol=SASL_PLAINTEXT \
    --sasl.username=admin \
    --sasl.password=secret
```

### Running against a remote Kafka cluster secured with SASL Plain

Executing migrations on a remote Kafka cluster is done simply by providing the `--bootstrap-servers` argument (which defaults to `localhost:9090`).

This example is compatible with a remotely managed clusters like Confluent Cloud. Refer to the (doc/confluent-cloud.md)[documentation section on Confluent Cloud] for instructions on how to provision an API key for Topic Notary to work properly.

```
./cli/target/topic-notary-cli.jar \
  --environment-name=my-env \
  --migrations-dir=../../doc/sample-migrations \
  --bootstrap-servers=my-kafka-bootstrap-servers:9092 \
  --sasl.mechanism=PLAIN \
  --security.protocol=SASL_PLAINTEXT \
  --sasl.username=admin \
  --sasl.password=secret
```

### Running with environment variables

Topic Notary is using Spring Boot under the hood. So any configuration passed as a command argument may be configured as an environment variables as shown in this example:

```
export BOOTSTRAP_SERVERS=my-kafka-bootstrap-servers:9092
export ENVIRONMENT_NAME=my-env
export MIGRATIONS_DIR=../../doc/sample-migrations
export SASL_MECHANISM=PLAIN
export SECURITY_PROTOCOL=SASL_PLAINTEXT
export SASL_USERNAME=admin
export SASL_PASSWORD=secret

./cli/target/topic-notary-cli.jar
```

### Sample output

```
2020-11-05 09:39:51.880  INFO 63192 --- [           main] c.g.d.topicnotary.cli.TopicNotaryCli     : Starting TopicNotaryCli v0.0.1-SNAPSHOT on Daniels-MacBook-Pro.local with PID 63192
2020-11-05 09:39:51.882  INFO 63192 --- [           main] c.g.d.topicnotary.cli.TopicNotaryCli     : No active profile set, falling back to default profiles: default
2020-11-05 09:40:00.822  INFO 63192 --- [           main] .d.t.c.m.r.MigrationEntryKafkaRepository : Creating topic _migration.
2020-11-05 09:40:08.466  INFO 63192 --- [           main] c.g.d.t.c.m.MigrationServiceImpl         : Successfully applied migration 0.0.1-dev.1 - Initial Commit.
2020-11-05 09:40:08.547  INFO 63192 --- [           main] c.g.d.t.c.m.MigrationServiceImpl         : Successfully applied migration 0.0.1-dev.2 - ACL Deletion test.
2020-11-05 09:40:08.639  INFO 63192 --- [           main] c.g.d.t.c.m.MigrationServiceImpl         : Successfully applied migration 0.1.0 - Ordering Test 1.
2020-11-05 09:40:08.733  INFO 63192 --- [           main] c.g.d.t.c.m.MigrationServiceImpl         : Successfully applied migration 0.1.1 - Ordering Test 2.
2020-11-05 09:40:08.820  INFO 63192 --- [           main] c.g.d.t.c.m.MigrationServiceImpl         : Successfully applied migration 0.2.0 - Ordering Test 3.
2020-11-05 09:40:08.820  INFO 63192 --- [           main] c.g.d.t.c.m.MigrationServiceImpl         : Completed topics migration.
2020-11-05 09:40:08.931  INFO 63192 --- [           main] c.g.d.topicnotary.cli.TopicNotaryCli     : Started TopicNotaryCli in 17.434 seconds (JVM running for 17.82)
```