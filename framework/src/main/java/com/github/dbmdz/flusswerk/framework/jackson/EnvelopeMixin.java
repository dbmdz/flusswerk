package com.github.dbmdz.flusswerk.framework.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface EnvelopeMixin {

  @JsonIgnore
  String getBody();

  @JsonIgnore
  long getDeliveryTag();
}
