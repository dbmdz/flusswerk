# Digital Collections Workflow Engine

The digital collections workflow engine makes it easy to create multithreaded workers for read-transform-write chains (aka ETL jobs).
 
 
For small flows you can just use lambdas:

```java
Flow<String, String> flow = new FlowBuilder<String, String>()
    .read("testQueue", message -> message.get("orderId"))
    .transform(orderId -> orderService.process(orderId))
    .write("output", receipt -> {
      DefaultMessage message = new DefaultMessage("order completed");
      message.put("receiptId", receipt.getId());
      return message;
    })
    .build();
```  

For larger flows you can also use classes implementing the Java functional interfaces:


```java
import java.util.function.Function

class Reader implements Function<Message, Order> { /* ... */ }

class Transformer implements Function<Order, Receipt> { /* ... */ }

class Writer implements Function<Receipt, Message> { /* ... */ }

Flow<String, String> flow = new FlowBuilder<String, String>()
    .read("testQueue", new Reader())
    .transform(new Transformer())
    .write("output", new Writer())
    .build();
```  

Here it is important that the classes do not keep a state (or do so in a threadsafe way) because the function's apply method is called in multile threads at the same time. 
