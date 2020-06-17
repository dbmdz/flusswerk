package com.github.dbmdz.flusswerk.spring.boot.example;

import com.github.dbmdz.flusswerk.framework.EnableFlusswerk;
import com.github.dbmdz.flusswerk.framework.FlusswerkApplication;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Example workflow job using Spring boot. */
@SpringBootApplication
@EnableFlusswerk
public class Application extends FlusswerkApplication {

  @Autowired
  public Application(Engine engine) {
    super(engine);
  }

  public static void main(String[] args) {
    FlusswerkApplication.run(Application.class, args);
  }
}
