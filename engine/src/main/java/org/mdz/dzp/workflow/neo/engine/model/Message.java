package org.mdz.dzp.workflow.neo.engine.model;

import java.time.LocalDateTime;

public interface Message<ID> {

  String getType();

  long getDeliveryTag();

  void setDeliveryTag(long deliveryTag);

  LocalDateTime getTimestamp();

  void setTimestamp(LocalDateTime timestamp);

  String getBody();

  void setBody(String body);

  int getRetries();

  void setRetries(int retries);

  ID getId();

  void setId(ID id);

}
