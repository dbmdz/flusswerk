# Flusswerk - Digital Collections Workflow Engine

[![Javadocs](https://javadoc.io/badge/de.digitalcollections.flusswerk/dc-flusswerk-parent.svg)](https://javadoc.io/doc/de.digitalcollections.flusswerk/dc-flusswerk-parent)
[![Build status](https://gitlab.com/dbmdz/flusswerk/badges/master/pipeline.svg)](https://gitlab.com/dbmdz/flusswerk/commits/master)
[![Codecov](https://img.shields.io/codecov/c/github/dbmdz/flusswerk/master.svg)](https://codecov.io/gh/dbmdz/flusswerk)
[![License](https://img.shields.io/github/license/dbmdz/flusswerk.svg)](LICENSE)
[![GitHub release](https://img.shields.io/github/release/dbmdz/flusswerk.svg)](https://github.com/dbmdz/flusswerk/releases)
[![Maven Central](https://img.shields.io/maven-central/v/de.digitalcollections.flusswerk/dc-flusswerk-parent.svg)](https://search.maven.org/search?q=a:dc-flusswerk-parent)

Flusswerk makes it easy to create multithreaded workers for read-transform-write chains (aka ETL jobs). Workflows are coordinated via RabbitMQ, so it's easy to create chains of independent workflow jobs (each a new Java Application).

Maven:

```xml
<dependency>
  <groupId>com.github.dbmdz.flusswerk</groupId>
  <artifactId>framework</artifactId>
  <version>4.0.0</version>
</dependency>
```

Gradle:

```groovy
dependencies {
    compile group: 'de.digitalcollections.flusswerk', name: 'dc-flusswerk-engine', version: '2.2.1'
}
``` 
 
 
## Requirements
 
Required libraries are Jackson and RabbitMQ Java API, the minimal Java version is 8 (will move to 9 in a few months). The Flusswerk engine itself *never requires Spring*, but there are examples how to integrate the engine in a Spring Boot Application. 

## Migration from version 1.x to 2.x

Starting with version 2.0.0, the interface for flows sending many messages has been simplified. The writer now can use the message class generics directly: 

```java
class Writer implements java.util.function.Function<T, Collection<? extends Message>> {
  @Override
  public Collection<? extends Message> apply(T value) {
    // ...
  } 
}
```

gets now 
 
```java
class Writer implements java.util.function.Function<T, Collection<Message>> {
  @Override
  public Collection<Message> apply(T value) {
    // ...
  } 
}
```

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
        .writeAndSend(value -> new DefaultMessage("your.id").put("value", value))
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
      return new DefaultMessage("your.id").put("value", output);
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
        .writeAndSend(new Writer())
        .build();
    
    Engine engine = new Engine(messageBroker, flow);
    engine.start();
  }
}
```

Depending if you want to want to send one message, multiple messages or no message at all, the FlowBuilder has suitable API methods:


 - `flowBuilder.write(Consumer<T>)` processes values of type `T`, but does not send messages returned by the writer.
 - `flowBuilder.writeAndSend(Function<T, Message>)` processes values of type `T`, and sends the message returned by the writer to the default output queue.
 - `flowBuilder.writeAndSendMany(Function<T, List<Message>>)` processes values of type `T`, and sends all messages in the list returned by the writer to the default output queue.

It is always possible to use `MessageBroker.send("some.queue", Message)` anywhere to manually [send messages to arbitrary queues](#sending-messages-to-arbitrary-queues).

## Isolating messages with suppliers

By using the same instance of a reader, transformer or writer for every message it is easy to introduce side-effects and thread-safety issues. Therefore, `FlowBuilder` supports Suppliers to create a new instance for every message.  

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
        .read(new ReaderSupplier())
        .transform(() -> new Transformer())
        .writeAndSend(new Writer())
        .build();
    // ...
  }
}
```  

In case, you want to use autowiring from Spring, you should annotate the `Reader`, `Transformer` and/or `Writer` beans with `@Scope("prototype")` and
use the following bean configuration:

```java
  // ...
  @Bean
  public Flow flow(ObjectFactory<Reader> readerObjectFactory,
      ObjectFactory<Transformer> transformerObjectFactory,
      ObjectFactory<Writer> writerObjectFactory) {
    return new FlowBuilder<DefaultMessage, String, String>()
        .read(() -> readerObjectFactory.getObject())
        .transform(() -> transformerObjectFactory.getObject())
        .write(() -> writerObjectFactory.getObject())
        .build();
  }
  // ...
```


## Sending messages to arbitrary queues

The Writer always sends a message to the defined output queue, which satisfies most use cases. For more complex workflows the `MessageBroker` can be used to send messages to any queue you like:

```java
class Writer implements Function<String, Message> {
  private final MessageBroker messageBroker;
  public Writer(MessageBroker messageBroker) {
    this.messageBroker = requireNonNull(messageBroker);
  }
  public Message apply(String value) {
    // ...
    // Notify other workflow jobs
    messageBroker.send("ocr", new DefaultMessage("1000001"));
    messageBroker.send("iiif", new DefaultMessage("1000001"));
    messageBroker.send("import", new DefaultMessage("1000001"));
    // ...
  }
}
```

## Cleanup

If you want to perform cleanups after processing of the message, e.g. for triggering a garbage collection, you can use the
```cleanup()``` method of the FlowBuilder:

```java
class Application {
  public static void main(String[] args) {
    MessageBroker messageBroker = new MessageBrokerBuilder()
        .readFrom("your.input.queue")
        .writeTo("your.output.queue")
        .build();
    
    Flow flow = new FlowBuilder<DefaultMessage, String, String>()
        .read(new Reader())
        .transform(new Transformer())
        .writeAndSend(new Writer())
        .cleanup(() -> Runtime.getRuntime().gc())
        .build();
    
    Engine engine = new Engine(messageBroker, flow);
    engine.start();
  }
}
```

## Propagate FlowIds

If incoming and outgoing message classes implement `HasFlowId`, automated propagation of flow ids is available. If the option `flowBuilder.propagateFlowId(true)` is set, Flusswerk copies flow ids from incoming to all outgoing messages.

## Message Types

It is recommended to implement your own message class to add type safety, expressiveness and easy to
understand algorithms:

```java
public class ExampleMessage extends FlusswerkMessage<Integer> {

  private int priority;

  private String[] tags;

  public PowerMessage(Integer id, int priority, String... tags) {
    this.priority = priority;
    this.tags = requireNonNullElseGet(tags, () -> new String[]{});
  }

  public int getPriority() {
    return priority;
  }

  public String[] getTags() {
    return tags;
  }

  @Override
  public String toString() {
    return String.format("ExampleMessage{%d, %d, %s}", id, priority, Arrays.toString(tags));
  }
}
```

The custom message implementation needs to be registered with the MessageBrokerBuilder:

```java
class Application {
  public static void main(String[] args) {
    MessageBroker messageBroker = new MessageBrokerBuilder()
        .useMessageClass(ExampleMessage.class)
        .build();
    /* ... */
  }
}
```

For a more fine grained serialization control, a custom Jackson mixin is also possible:

```java
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_EMPTY)
public interface ExampleMessageMixin {}
```

```java
class Application {
  public static void main(String[] args) {
    MessageBroker messageBroker = new MessageBrokerBuilder()
        .useMessageClass(ExampleMessage.class, ExampleMessageMixin.class)
        .build();
    /* ... */
  }
}
```

## Use custom reporting

By default, the message statuses (success, temporarily failed, finally failed) are just logged by the Logger of `DefaultProcessReport`. If you want to customize this (e.g. for writing structured logging messages with only specific information), you can provide a custom implementation of the `ProcessReport` interface and pass this to the Engine:

```java
class Application {
  public static void main(String[] args) {
    //...
    Engine engine = new Engine(messageBroker, flow, new MyProcessReport());
    engine.start();
  }
}
```

## Failure Policies

As default every input queue gets a retry and a failed queue with the same name as the input queue with suffixes `.failure` and `.retry`. Every message is retried 5 times and then moved to the failed queue.

To customize this behaviour one can set `FailurePolicies`:

```java
class Application {
  public static void main(String[] args) {
    MessageBroker messageBroker = new MessageBrokerBuilder()
        .failurePolicy(new FailurePolicies("inputQueue", "retryRoutingKey", "failureRoutingKey", 42))
        .build();
    /* ... */
  }
}
``` 

If messages should not be retried, set `retryRoutingKey` to `null`. If permanently failing messages should be discarded, set `failureRoutingKey` to `null`.

### Failure control by exceptions

If your read/transform/write implementations, you can control the failure handling by failing a message temporarily or finally.

If a message shall fail finally, just throw an `FinallyFailedProcessException`; if you want to fail temporarily and schedule a retry, throw a `RetriableProcessException`.


## Multithreading

If you want to process more than one message at the same time, you can customize, how many threads the engine uses:

```java
class Application {
  public static void main(String[] args) {
    int concurrentWorkers = 42;
    
    //...
    Engine engine = new Engine(messageBroker, flow, concurrentWorkers);
    engine.start();
    //...
  }
}
```

## Collect metrics

To collect metrics for a flow, one can register a `Consumer<FlowStatus>` with the FlowBuilder:

```java
Flow flow = new FlowBuilder<DefaultMessage, String, String>()
        // ...
        .measure(metrics)
        .build();
``` 

If you use Spring Boot, there is a complete support for Micrometer Metrics 

```java
@Configuration
class FlusswerkConfig {

  @Bean
  public Flow<Message, String, String> flow(
      Reader reader,
      Transformer transformer,
      Writer writer,
      Metrics metrics) {
    return FlowBuilder.with(Message.class, String.class, String.class)
        // reader, transformer, writer,...
        .measure(metrics)
        .build();
  }

}
``` 

To implement your own Metrics Bean, either use
```java
class Metrics implements Consumer<FlowStatus> {

  public void accept(FlowStatus flowStatus) {
    // ...
  }

}
``` 

or subclass Flusswerk Metrics-Bean:

```java
@Component
public class Metrics
    extends Metrics {

  private final Counter buzz;

  public Metrics(MeterFactory meterFactory) {
    super(meterFactory);

    // your definitions here
    this.buzz = meterFactory.counter("buzz");
  }

  public void accept(FlowStatus flowStatus) {
    super.accept(flowStatus);
  }

  public void buzz() {
    this.buzz.increment();
  }
}
```


## MessageBrokerBuilder properties

### RabbitMQ connection

| Property                          | Meaning                                       |
| --------------------------------- | --------------------------------------------- | 
| `connectTo(String connectionStr)` | RabbitMQ host name and port, separated by a colon (default `localhost:5672`). Can also be a list, separated by comma |
| `username(String username)`       | Username for authentication (default `guest`) |
| `password(String password)`       | Password for authentication (default `guest`) |
| `virtualHost(String virtualHost)` | RabbitMQ virtual host                         |

### Message Routing

Everything will be created if it does not exist:

| Property                                        | default               | Meaning     |
| ----------------------------------------------- | --------------------- | ------------------------------------------------------------------- | 
| `readFrom(String inputQueue)`                   | -                     | Queue to read incoming messages from                                |
| `writeTo(String outputRoutingKey)`              | -                     | Queue to write outgoing messages to                                 |
| `addFailurePolicy(FailurePolicy policy)`        | behaviour as in section *Failure Policies*  | Policy for retrying messages                  |
| `exchange(String exchange)`                     | `workflow`            | RabbitMQ exchange for routing messages                              |
| `deadLetterExchange(String deadLetterExchange)` | `workflow.retry`      | RabbitMQ dead letter exchange to reroute failed messages            |
| `maxRetries(int number)`                        | `5`                   | The number of retries until a message is routed to the failed queue |

### Jackson Mapping

| Property                                                                       | Meaning                                                   |
| ------------------------------------------------------------------------------ | --------------------------------------------------------- |
| `jacksonModules(Module... modules)`                                            | A list of Jackson Modules to configure arbitrary Mappings |
| `messageMapping(Class<? extends Message> messageClass, Class<?> messageMixin)` | Set a custom message type and its Jackson Mapping         |
