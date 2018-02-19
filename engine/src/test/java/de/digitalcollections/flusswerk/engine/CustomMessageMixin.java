package de.digitalcollections.flusswerk.engine;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface CustomMessageMixin {

  @JsonIgnore
  String getType();

  @JsonIgnore
  String getId();

}
