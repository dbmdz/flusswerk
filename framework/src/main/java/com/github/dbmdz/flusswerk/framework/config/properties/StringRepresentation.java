package com.github.dbmdz.flusswerk.framework.config.properties;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/** Creates a nice string representation for hierarchical data structures. */
class StringRepresentation {

  private static final StringRepresentation INSTANCE = new StringRepresentation();

  private final Yaml yaml;

  private StringRepresentation() {
    DumperOptions options = new DumperOptions();
    options.setAllowReadOnlyProperties(true);
    options.setAllowUnicode(true);
    yaml = new Yaml(options);
  }

  public static String of(Object o) {
    return INSTANCE.yaml.dump(o);
  }
}
