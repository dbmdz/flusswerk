package com.github.dbmdz.flusswerk.framework.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.util.StringUtils;

@ConstructorBinding
@ConfigurationProperties(prefix = "flusswerk.redis")
public class RedisProperties {

  private final String address;
  private final String password;

  public RedisProperties(String address, String password) {
    if (StringUtils.hasText(address)) {
      this.address = address.contains(":") ? address : address + ":6379";
    } else {
      this.address = null; // if Redis is not configured/used
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
