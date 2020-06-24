package com.github.dbmdz.flusswerk.framework.config.properties;

import java.util.Map;

/** Creates a nice string representation for hierarchical data structures. */
class StringRepresentation {

  private final StringBuilder stringBuilder;

  private StringRepresentation(String name) {
    stringBuilder = new StringBuilder(name);
    newline();
  }

  public static StringRepresentation of(Class<?> cls) {
    return new StringRepresentation(cls.getSimpleName());
  }

  public StringRepresentation property(String name, int value) {
    return property(name, Integer.toString(value));
  }

  /**
   * Add a property as key-value-pair with proper intendation.
   *
   * @param name The properties name.
   * @param value The properties value.
   * @return The <code>StringRepresentation</code> object to allow fluent method chaining.
   */
  public StringRepresentation property(String name, String value) {
    if (value == null) {
      value = "null";
    }
    indent();
    text(name);
    text(":");
    indent();
    if (value.contains("\n")) {
      newline();
      for (String line : value.split("\n")) {
        indent();
        text(line);
        newline();
      }
    } else {
      text(value);
      newline();
    }

    return this;
  }

  public StringRepresentation property(String name, Map<String, String> values) {
    if (values == null) {
      property(name, "null");
    }
    text(name);
        text(":");
    for (String key : values.keySet()) {
      indent();
      property(key, values.get(key));
    }
    return this;
  }

    private void indent() {
    stringBuilder.append("\t");
  }

  private void newline() {
    stringBuilder.append("\n");
  }

  private void text(String value) {
    stringBuilder.append(value);
  }

  public String toString() {
    return stringBuilder.toString();
  }

  /**
   * Creates a masked version of the property, hiding all but the first letter. The remaining
   * letters are replaced by exactly five "*" characters to conceal the value's true length.
   *
   * @param name The properties name.
   * @param value The properties value.
   * @return The <code>StringRepresentation</code> object to allow fluent method chaining.
   */
  public StringRepresentation maskedProperty(String name, String value) {
    stringBuilder.append("\n\t");
    stringBuilder.append(name);
    stringBuilder.append(":\t");
    stringBuilder.append(value, 0, 1);
    stringBuilder.append("*".repeat(5)); // Fixed number of stars to hide real password length
    return this;
  }
}
