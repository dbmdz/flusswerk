# Digital Collections Workflow Engine

[![Build Status](https://www.travis-ci.org/dbmdz/workflow.svg?branch=master)](https://www.travis-ci.org/dbmdz/workflow)
[![MIT License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![codebeat badge](https://codebeat.co/badges/8a7dab47-377e-428f-a46e-e3f9fb6cf68d)](https://codebeat.co/projects/github-com-dbmdz-workflow-master)
[![codecov](https://codecov.io/gh/dbmdz/workflow/branch/master/graph/badge.svg)](https://codecov.io/gh/dbmdz/workflow)
[![Javadocs](https://javadoc.io/badge/de.digitalcollections.workflow/dc-workflow-engine.svg)](https://javadoc.io/doc/de.digitalcollections.workflow/dc-workflow-engine)
[![Maven Central](https://img.shields.io/maven-central/v/de.digitalcollections.workflow/dc-workflow-engine.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22de.digitalcollections.workflow%22)

The digital collections workflow engine makes it easy to create multithreaded workers for read-transform-write chains (aka ETL jobs). Workflows are coordinated via RabbitMQ, so it's easy to create chains of independent workflow jobs (each a new Java Application).

Maven:

```xml
<dependency>
  <groupId>de.digitalcollections.workflow</groupId>
  <artifactId>dc-workflow-engine</artifactId>
  <version>0.3.0</version>
</dependency>
```

Gradle:

```groovy
dependencies {
    compile group: 'de.digitalcollections.workflow', name: 'dc-workflow-engine', version: '0.0.1-SNAPSHOT'
}
``` 
 
 
## Requirements
 
Required libraries are Jackson and RabbitMQ Java API, the minimal Java version is 8 (will move to 9 in a few months). The workflow engine itself will *never require Spring*, but there might be examples how to integrate the engine in a Spring Boot Application. 


## Basic setup

If you have an local RabbitMQ instance with default configurations up and running, creating your first worker is as easy as

```java
class Application {
  public static void main(String[] args) {
    MessageBroker messageBroker = new MessageBrokerBuilder()
        .readFrom("your.input.queue")
        .writeTo("your.output.queue")
        .build();
    
    Flow flow = new FlowBuilder<DefaultMessage, String, String>()
        .read(message -> message.get("value"))
        .transform(String::toUpperCase)
        .write(value -> DefaultMessage.withId("your.id").put("value", value))
        .build();
    
    Engine engine = new Engine(messageBroker, flow);
    engine.start();
  }
}
```

For more complex read, transform or write operations it is recommended to implement these as classes. Please keep in mind that these classes should not keep any state (or do so in a thread-safe way), as they are used from several worker threads at the same time:

```java
class Reader implements Function<DefaultMessage, String> {
  String apply(DefaultMessage message) {
    return message.get("value");
  }
}

class Transformer implements Function<String, String> {
  String apply(String input) {
    return input.toUpperCase();
  }  
}

class Writer implements Function<String, Message> { 
    Message apply(String output) {
      return DefaultMessage.withId("your.id").put("value", output);
    }
 }

class Application {
  public static void main(String[] args) {
    MessageBroker messageBroker = new MessageBrokerBuilder()
        .readFrom("your.input.queue")
        .writeTo("your.output.queue")
        .build();
    
    Flow flow = new FlowBuilder<DefaultMessage, String, String>()
        .read(new Reader())
        .transform(new Transformer())
        .write(new Writer())
        .build();
    
    Engine engine = new Engine(messageBroker, flow);
    engine.start();
  }
}
```

## Isolating messages with suppliers

By using the same instance of an reader, transformer or writer for every message it is easy to introduce side-effects and thread-safety issues. Therefore, `FlowBuilder` supports Suppliers to create a new instance for every message.  

This example uses a custom Supplier implementation for `Reader` and a supplier lambda for `Transformer`. The same instance of `Writer` is used for every message:

```java
class ReaderSupplier implements java.util.function.Supplier<Reader> {
    public Reader get() {
        return new Reader();
    }
}

class Application {
  public static void main(String[] args) {
    // ...
    Flow flow = new FlowBuilder<DefaultMessage, String, String>()
        .read(new ReaderSuppiler())
        .transform(() -> new Transformer())
        .write(new Writer())
        .build();
    // ...
  }
}
```  

## Sending messages to arbitrary queues

The Writer always sends a message to the defined output queue, which satisfies most use cases. For more complex workflows the MessageBroke can be used to send messages to any queue you like:

```java
class Writer implements Function<String, Message> {
  private final MessageBroker messageBroker;
  public Writer(MessageBroker messageBroker) {
    this.messageBroker = requireNonNull(messageBroker);
  }
  public Message apply(String value) {
    // ...
    // Notify other workflow jobs
    messageBroker.send("ocr", DefaultMessage.withId("1000001"));
    messageBroker.send("iiif", DefaultMessage.withId("1000001"));
    messageBroker.send("import", DefaultMessage.withId("1000001"));
    // ...
  }
}
```


## How to use a custom message implementation

The default message implementation `DefaultMessage` allows to set arbitrary key-value-pairs of type `String`. To use custom message it has to be registered:

```java
class Application {
  public static void main(String[] args) {
    MessageBroker messageBroker = new MessageBrokerBuilder()
        .messageMapping(CustomMessage.class, CustomMessageMixin.class)
        .build();
    /* ... */
  }
}
```  

## MessageBrokerBuilder properties

### RabbitMQ connection

| Property                          | Meaning                                       |
| --------------------------------- | --------------------------------------------- | 
| `hostName(String hostName)`       | RabbitMQ host name                            |
| `username(String username)`       | Username for authentication (default `guest`) |
| `password(String password)`       | Password for authentication (default `guest`) |
| `port(int port)`                  | RabbitMQ port                                 |
| `virtualHost(String virtualHost)` | RabbitMQ virtual host                         |

### Message Routing

Everything will be created if it does not exist:

| Property                                        | default               | Meaning     |
| ----------------------------------------------- | --------------------- | ------------------------------------------------------------------- | 
| `readFrom(String inputQueue)`                   | -                     | Queue to read incoming messages from                                |
| `writeTo(String outputRoutingKey)`              | -                     | Queue to write outgoing messages to                                 |
| `retryQueue(String name)`                       | `<inputQueue>.retry`  | Queue to store messages to retry                                    |
| `failedQueue(String name)`                      | `<inputQueue>.failed` | Queue to store permanently failed messages                          |
| `exchange(String exchange)`                     | `workflow`            | RabbitMQ exchange for routing messages                              |
| `deadLetterExchange(String deadLetterExchange)` | `workflow.retry`      | RabbitMQ dead letter exchange to reroute failed messages            |
| `maxRetries(int number)`                        | `5`                   | The number of retries until a message is routed to the failed queue |

### Jackson Mapping

| Property                                                                       | Meaning                                                   |
| ------------------------------------------------------------------------------ | --------------------------------------------------------- |
| `jacksonModules(Module... modules)`                                            | A list of Jackson Modules to configure arbitrary Mappings |
| `messageMapping(Class<? extends Message> messageClass, Class<?> messageMixin)` | Set a custom message type and its Jackson Mapping         |
