package de.digitalcollections.flusswerk.engine.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface EnvelopeMixin {

  @JsonIgnore
  String getBody();

  @JsonIgnore
  long getDeliveryTag();

}
