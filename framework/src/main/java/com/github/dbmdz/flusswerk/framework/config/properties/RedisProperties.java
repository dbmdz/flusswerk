package com.github.dbmdz.flusswerk.framework.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.util.StringUtils;

@ConstructorBinding
@ConfigurationProperties(prefix = "flusswerk.redis")
public class RedisProperties {

  private static final int DEFAULT_PORT = 6379;

  private final String address;
  private final String password;

  public RedisProperties(String address, String password) {
    if (!StringUtils.hasText(address)) {
      this.address = null; // if Redis is not configured/used
      this.password = null;
      return;
    }

    if (address.startsWith("redis://") || address.startsWith("rediss://")) {
      if (address.substring(8).contains(":")) {
        this.address = address;
      } else {
        this.address = address + ":" + DEFAULT_PORT;
      }
    } else {
      if (address.contains(":")) {
        this.address = "redis://" + address;
      } else {
        this.address = "redis://" + address + ":" + DEFAULT_PORT;
      }
    }

    this.password = password; // might be null if no authentication is used
  }

  public String getAddress() {
    return address;
  }

  public String getPassword() {
    return password;
  }

  public boolean redisIsAvailable() {
    return this.address != null;
  }
}
