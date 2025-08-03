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
    private Refresh refresh = new Refresh();

    @Data
    public static class Refresh {
      private boolean enabled = false;
      private long intervalMinutes = 60; // Default 1 hour
      private long delayMinutes = 60; // Initial delay before first refresh
    }
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
