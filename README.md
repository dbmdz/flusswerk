# Flusswerk - Digital Collections Workflow Engine

[![Javadocs](https://javadoc.io/badge/de.digitalcollections.flusswerk/dc-flusswerk-parent.svg)](https://javadoc.io/doc/de.digitalcollections.flusswerk/dc-flusswerk-parent)
[![License](https://img.shields.io/github/license/dbmdz/flusswerk.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/de.digitalcollections.flusswerk/dc-flusswerk-parent.svg)](https://search.maven.org/search?q=a:dc-flusswerk-parent)

Flusswerk makes it easy to create multi threaded workers for
read/transform/write chains (aka ETL jobs). Workflows are coordinated via
RabbitMQ, so it's easy to create chains of independent workflow jobs (each a new
Java application).

**Maven:**

```xml
<dependency>
  <groupId>com.github.dbmdz.flusswerk</groupId>
  <artifactId>framework</artifactId>
  <version>5.0.0</version>
</dependency>
```

**Gradle:**

```groovy
dependencies {
    compile group: 'com.github.dbmdz.flusswerk', name: 'flusswerk', version: '5.0.0'
}
``` 
 
 ## Getting started

To get started, clone or copy the [Flusswerk
Example](https://github.com/dbmdz/flusswerk-example) application.

 
## What's new in Flusswerk 5

- Flusswerk connections are now shown in RabbitMQ management UI
- structured logging now automatically contains fields `duration` and `duration_ms` that report the time your app spent on processing a certain message in seconds or milliseconds.
- The deprecated `tracing_id` has been removed
- The deprecated `Envelope.timestamp` has been removed in favor of `Envelope.created` which is now of type `Instant`.
- Constructor of FlusswerkApplication now takes `Optional<Engine>` as an argument (call as `super(Optional.of(engine));`).
- Options for centralized locking using Redis have been removed

[FlowSpec]:
framework/src/main/java/com/github/dbmdz/flusswerk/framework/flow/FlowSpec.java
[IncomingMessageType]:
framework/src/main/java/com/github/dbmdz/flusswerk/framework/model/IncomingMessageType.java

## The Big Picture

A typical Flusswerk application has three parts:

 - Messages
 - the data processing flow (usually a `Reader`, a `Transformer` and a `Writer`)
 - Some Spring Boot glue code
 - Spring Boot `application.yml` to configure all the Flusswerk things

Usually it is also useful to define your own data model classes, although that
is not strictly required.

Other optional parts are

 - Custom metrics collection
 - Custom logging formats (aka `ProcessReport`)

### Messages

Message classes are for a sending and receiving data from RabbitMQ. All Message
classes extend [Message][Message], which automatically forwards tracing ids from
incoming to outgoing messages (if you set the tracing id by hand, it will not be
overwritten).

```java
class IndexMessage implements Message {

  private String documentId;

  public IndexMessage(String documentId) {
    this.documentId = requireNonNull(documentId);
  }

  public String getId() { ... }

  public boolean equals(Object other) { ... }

  public int hashCode() { ... }

}
```

Register the type for the incoming message, so it gets automatically
deserialized:

```java
@Bean
public IncomingMessageType incomingMessageType() {
  return new IncomingMessageType(IndexMessage.class);
}
```

[Message]: framework/src/main/java/com/github/dbmdz/flusswerk/framework/model/Message.java


### Configuration

All configuration magic happens in Spring's `application.yml`.

A minimal configuration might look like:

```yml
# Default spring profile is local
spring:
  application:
    name: flusswerk-example

flusswerk:
  routing:
    incoming:
      - search.index
    outgoing:
      default: search.publish
```

This defaults to connecting to RabbitMQ `localhost:5672`, with user and password
`guest`, five threads and retrying a message five times. The only outgoing route
defined is default, which is used by Flusswerk to automatically send messages.
For most applications these are sensible defaults and works out of the box for
local testing.

The connection information can be overwritten for different environments using
Spring Boot profiles:

```yml
---
spring:
  profiles: production
flusswerk:
  rabbitmq:
    hosts:
      - rabbitmq.stg
    username: secret
    password: secret
```

The sections of the `Flusswerk` configuration

`processing` - control of processing

| property  | default |                                                  |
| --------- | ------- | ------------------------------------------------ |
| `threads` | 5       | Number of threads to use for parallel processing |

`rabbitmq` - Connection to RabbitMQ:

| property    | default     |                             |
| ----------- | ----------- | --------------------------- |
| `hosts`     | `localhost` | list of hosts to connect to |
| `username`  | `guest`     | RabbitMQ username           |
| `passwords` | `guest`     | RabbitMQ password           |


`routing` - Messages in and out

| property                | default                 |                                                                  |
| ----------------------- | ----------------------- | ---------------------------------------------------------------- |
| `incoming`              | `–`                     | list of queues to read from in order                             |
| `outgoing`              | `–`                     | routes to send messages to (format 'name: topic')                |
| `exchange`              | `flusswerk_default`     | default exchange for all queues                                  |
| `dead letter exchange`  | `<exchange> + ".retry"` | default dead letter exchange for all queues                      |
| `exchanges`             | `-`                     | `queue: exchange name` to override default exchanges             |
| `dead letter exchanges` | `<exchange> + ".retry"` | `queue: exchange name` to override default dead letter exchanges |
| `failure policies`      | `default`               | how to handle messages with processing errors                    |

`routing.failure policies` - how to handle messages with processing errors

| property           | default |                                                              |
| ------------------ | ------- | ------------------------------------------------------------ |
| `retries`          | `5`     | how many times to retry                                      |
| `retryRoutingKey`  | `–`     | where to send messages to retry later *(dead lettering)*     |
| `failedRoutingKey` | `–`     | where to send messages to that should not be processed again |
| `backoff`          | `–`     | how long to wait until retrying a message                    |

`monitoring` - Prometheus settings

| property | default     |                               |
| -------- | ----------- | ----------------------------- |
| `prefix` | `flusswerk` | prefix for prometheus metrics |


### Data Processing

To set up your data processing flow, define a Spring bean of type FlowSpec:

```java
@Bean
public FlowSpec flowSpec(Reader reader, Transformer transformer, Writer writer) {
  return FlowBuilder.flow(IndexMessage.class, Document.class, IndexDocument.class)
      .reader(reader)
      .transformer(transformer)
      .writerSendingMessage(writer)
      .build();
}
```

With the `Reader`, `Transformer` and `Writer` implementing the `Function` interface:

|               |                                     |                                                                         |
| ------------- | ----------------------------------- | ----------------------------------------------------------------------- |
| `Reader`      | `Function<IndexMessage, Document>`  | loads document from storage                                             |
| `Transformer` | `Function<Document, IndexDocument>` | uses `Document` to build up the data structure needed for indexing      |
| `Writer`      | `Function<IndexDocument, Message>`  | sends indexes the data and returns a message for the next workflow step |


## Best Practices

### Stateless Processing

All classes that do data processing (Reader, Transformer, Writer,...) should be
stateless. This has two reasons:

First, it makes your code thread-safe and multiprocessing easy without you
having to even think about it. Just keep it stateless and fly!

Second, it makes testing a breeze: You throw in data and check the data that
comes out. Things can go wrong? Just check if your code throws the right
exceptions. Wherever you need to interact with external services, mock the
behaviour, and your good to go (the Flusswerk tests make heavy use of Mockito,
btw.).

If you absolutely have to introduce state, make sure your code is thread-safe.


### Immutable Data

Wherever sensible, make your data classes immutable - set everything via the
constructor and avoid setters. Implement `equals()` and `hashCode()`. This leads
usually to more readable code, and makes writing tests much easier. This applies
to Message classes and to the classes that contain data. 

Your particular data processing needs to build your data over time and can't be
immutable? Think again if that is the best way, but don't worry too much.



## Manual Interaction with RabbitMQ

For manual interaction with RabbitMQ there is a Spring component with the same
class:

| `RabbitMQ`       |                                                                         |
| ---------------- | ----------------------------------------------------------------------- |
| `ack(Message)`   | acknowledges a `Message` received from a `Queue`                        |
| `queue(String)`  | returns the `Queue` instance to interact with a queue of the given name |
| `topic(Message)` | returns the `Topic` instance for the given name to send messages to     |
| `route(Message)` | returns the `Topic` instance for the given route from `application.yml` |


## Error Handling

Any data processing can go wrong. Flusswerk supports two error handling modes:

 1. stop processing for a message completely. This behaviour is triggered by a
    [StopProcessingException][StopProcessingException].
 2. retry processing for a message later. This behaviour is triggered by a
    [RetryProcessingException][RetryProcessingException] or any other
    [RuntimeException][RuntimeException].

The default retry behaviour is to wait 30 seconds between retries and try up to
5 times. If processing a message still keeps failing, it is then treated like as
if a StopProcessingException had been thrown and will be routed to a failed
queue.

For more fine-grained control, see the configuration parameters for
`flusswerk.routing.failure policies`.

[StopProcessingException]:
framework/src/main/java/com/github/dbmdz/flusswerk/framework/exceptions/StopProcessingException.java
[RetryProcessingException]:
framework/src/main/java/com/github/dbmdz/flusswerk/framework/exceptions/RetryProcessingException.java
[RuntimeException]:
https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/RuntimeException.html


### Skip Processing

Processing can be skipped for a message by throwing a `SkipProcessingException` at any point in your
code. Log messages and metrics will have `status=skip` to indicate that processing was skipped. 

## Collecting Metrics

Every Flusswerk application provides base metrics via as a
[Prometheus][Prometheus] endpoint:

|                             |                                                         |
| --------------------------- | ------------------------------------------------------- |
| `flusswerk.processed.items` | total number of processed items since application start |
| `flusswerk.execution.time`  | total amount of time spend on processing these items    |

To include custom metrics, get counters via [MeterFactory][MeterFactory]. A bean
of type [FlowMetrics][FlowMetrics] can also consume execution information of
single flows (best to extend [BaseMetrics][BaseMetrics] for that). 


The prometheus endpoint is available at `/actuator/prometheus`.


[BaseMetrics]: framework/src/main/java/com/github/dbmdz/flusswerk/framework/monitoring/BaseMetrics.java
[FlowMetrics]: framework/src/main/java/com/github/dbmdz/flusswerk/framework/monitoring/FlowMetrics.java
[Prometheus]: https://prometheus.io/
[MeterFactory]: framework/src/main/java/com/github/dbmdz/flusswerk/framework/monitoring/MeterFactory.java


## Customize Logging

To customize log messages, provide a bean of type [ProcessReport](framework/src/main/java/com/github/dbmdz/flusswerk/framework/reporting/ProcessReport.java).
