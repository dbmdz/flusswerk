package com.github.dbmdz.flusswerk.integration;

import com.github.dbmdz.flusswerk.framework.config.properties.RedisProperties;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedisUtil {

  private static final String KEY_SPACE = "flusswerk";

  private final RedissonClient redissonClient;

  public RedisUtil(RedisProperties redisProperties) {
    Config config = new Config();
    config
        .useSingleServer()
        .setAddress(redisProperties.getAddress())
        .setPassword(redisProperties.getPassword());
    this.redissonClient = Redisson.create(config);
  }

  public RedissonClient getRedissonClient() {
    return redissonClient;
  }

  public RLock getRLock(String key) {
    return redissonClient.getLock(KEY_SPACE + "::" + key);
  }

  public void deleteAll() {
    redissonClient.getKeys().deleteByPattern(KEY_SPACE + "::*");
  }
}
