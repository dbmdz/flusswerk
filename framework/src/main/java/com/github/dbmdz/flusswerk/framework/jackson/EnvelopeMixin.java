package com.github.dbmdz.flusswerk.framework.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public interface EnvelopeMixin {

  @JsonIgnore
  String getBody();

  @JsonIgnore
  long getDeliveryTag();
}
