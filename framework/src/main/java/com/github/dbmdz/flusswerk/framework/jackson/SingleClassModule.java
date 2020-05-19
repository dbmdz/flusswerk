package com.github.dbmdz.flusswerk.framework.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class SingleClassModule extends SimpleModule {

  public SingleClassModule(Class<?> clazz, Class<?> mixin) {
    setMixInAnnotation(clazz, mixin);
  }
}