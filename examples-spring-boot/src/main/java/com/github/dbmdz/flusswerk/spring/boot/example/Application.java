package com.github.dbmdz.flusswerk.spring.boot.example;

import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.spring.boot.starter.EnableFlusswerk;
import com.github.dbmdz.flusswerk.spring.boot.starter.FlusswerkApplication;
import com.github.dbmdz.flusswerk.spring.boot.starter.FlusswerkProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Example workflow job using Spring boot. */
@SpringBootApplication
@EnableFlusswerk
public class Application extends FlusswerkApplication {

  private static Logger LOGGER = LoggerFactory.getLogger(Application.class);

  private FlusswerkProperties flusswerkProperties;

  @Autowired
  public Application(FlusswerkProperties flusswerkProperties, Engine engine) {
    super(engine);
    LOGGER.info(
        "Creating Flusswerk application for {}",
        flusswerkProperties.getConnection().getConnectTo());
  }

  public static void main(String[] args) {
    FlusswerkApplication.run(Application.class, args);
  }
}
