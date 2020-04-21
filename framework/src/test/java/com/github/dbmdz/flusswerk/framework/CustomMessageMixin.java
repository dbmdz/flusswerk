package com.github.dbmdz.flusswerk.framework;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface CustomMessageMixin {

  @JsonIgnore
  String getType();

  @JsonIgnore
  String getId();
}
