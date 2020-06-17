package com.github.dbmdz.flusswerk.spring.boot.example;

import com.github.dbmdz.flusswerk.framework.config.properties.FlusswerkProperties;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.EnableFlusswerk;
import com.github.dbmdz.flusswerk.framework.FlusswerkApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Example workflow job using Spring boot. */
@SpringBootApplication
@EnableFlusswerk
public class Application extends FlusswerkApplication {

  private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

  @Autowired
  public Application(FlusswerkProperties flusswerkProperties, Engine engine) {
    super(engine);
    var connection = flusswerkProperties.getConnection();
    LOGGER.info(
        "Creating Flusswerk application for {}:{}",
        connection.getHost(), connection.getPort());
  }

  public static void main(String[] args) {
    FlusswerkApplication.run(Application.class, args);
  }
}
