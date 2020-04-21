package de.digitalcollections.flusswerk.engine.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_EMPTY)
public interface DefaultMessageMixin {

  @JsonIgnore
  int getDeliveryTag();

  @JsonIgnore
  int getBody();
}
