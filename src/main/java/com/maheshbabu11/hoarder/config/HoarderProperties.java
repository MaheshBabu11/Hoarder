package com.maheshbabu11.hoarder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "hoarder")
public class HoarderProperties {

  private Cache cache = new Cache();
  private Logging logging = new Logging();

  @Data
  public static class Cache {
    private boolean enabled = true;
  }

  @Data
  public static class Logging {
    private boolean enabled = true;
    private LogLevel level = LogLevel.INFO;
  }

  public enum LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR
  }
}
