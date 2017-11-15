package org.mdz.dzp.workflow.neo.engine.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface DefaultMessageMixin {

  @JsonIgnore
  int getDeliveryTag();


  @JsonIgnore
  int getBody();

}
