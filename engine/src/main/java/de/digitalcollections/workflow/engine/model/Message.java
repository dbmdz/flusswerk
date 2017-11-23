package de.digitalcollections.workflow.engine.model;

public interface Message<ID> {

  Meta getMeta();

  String getType();

  ID getId();

}
