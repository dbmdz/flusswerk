package org.mdz.dzp.workflow.neo.engine.model;

public interface Message<ID> {

  String getType();

  long getDeliveryTag();

  void setDeliveryTag(long deliveryTag);

  String getBody();

  void setBody(String body);

  int getRetries();

  void setRetries(int retries);

  ID getId();

  void setId(ID id);

}
