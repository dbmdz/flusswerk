package de.digitalcollections.flusswerk.spring.boot.starter;

/**
 * Creates a nice string representation for hierarchical data structures.
 */
class StringRepresentation {

  private StringBuilder stringBuilder;

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

  public StringRepresentation maskedProperty(String name, String value) {
    stringBuilder.append("\n\t");
    stringBuilder.append(name);
    stringBuilder.append(":\t");
    stringBuilder.append(value, 0, 1);
    stringBuilder.append("*".repeat(5)); // Fixed number of stars to hide real password length
    return this;
  }

}
