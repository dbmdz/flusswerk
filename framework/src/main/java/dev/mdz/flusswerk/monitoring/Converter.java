package dev.mdz.flusswerk.monitoring;

public class Converter {
  public static double ns_to_seconds(long value) {
    return value / 1e9;
  }

  public static double ns_to_milliseconds(long value) {
    return value / 1e6;
  }
}
