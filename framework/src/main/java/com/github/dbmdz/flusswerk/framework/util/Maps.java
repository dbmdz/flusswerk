package com.github.dbmdz.flusswerk.framework.util;

import java.util.HashMap;
import java.util.Map;

// TODO Replace with Map.of(...) when switching to Java 9
public class Maps {

  public static <K, V> Map<K, V> of(K key, V value) {
    Map<K, V> result = new HashMap<>();
    result.put(key, value);
    return result;
  }

  public static <K, V> Map<K, V> of(K key1, V value1, K key2, V value2) {
    Map<K, V> result = new HashMap<>();
    result.put(key1, value1);
    result.put(key2, value2);
    return result;
  }
}
