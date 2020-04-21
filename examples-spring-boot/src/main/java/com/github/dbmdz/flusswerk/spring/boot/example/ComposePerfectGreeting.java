package com.github.dbmdz.flusswerk.spring.boot.example;

import java.util.function.Function;

public class ComposePerfectGreeting implements Function<String, String> {

  @Override
  public String apply(String name) {
    return String.format(
        "Behold, %s, my friend and be greeted as wonderful as no one has been greeted before!",
        name);
  }
}
