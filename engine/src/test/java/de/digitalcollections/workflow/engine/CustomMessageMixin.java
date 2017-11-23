package de.digitalcollections.workflow.engine;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface CustomMessageMixin {

  @JsonIgnore
  String getType();

  @JsonIgnore
  String getId();

}
