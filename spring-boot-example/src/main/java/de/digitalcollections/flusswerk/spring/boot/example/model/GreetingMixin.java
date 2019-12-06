package de.digitalcollections.flusswerk.spring.boot.example.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_EMPTY)
public interface GreetingMixin {

  @JsonIgnore
  int getDeliveryTag();

  @JsonIgnore
  int getBody();
}
