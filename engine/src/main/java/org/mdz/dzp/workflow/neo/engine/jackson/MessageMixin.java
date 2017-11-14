package org.mdz.dzp.workflow.neo.engine.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface MessageMixin {

  @JsonIgnore
  int getDeliveryTag();


  @JsonIgnore
  int getBody();

}
