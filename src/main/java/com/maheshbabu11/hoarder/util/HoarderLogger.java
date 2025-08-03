package com.maheshbabu11.hoarder.util;

import com.maheshbabu11.hoarder.config.HoarderProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class HoarderLogger {

  private static final Logger logger = LoggerFactory.getLogger("com.maheshbabu11.hoarder");
  private final HoarderProperties hoarderProperties;

  public HoarderLogger(HoarderProperties hoarderProperties) {
    this.hoarderProperties = hoarderProperties;
  }

  public void info(Class<?> clazz, String message, Object... args) {
    if (isLoggingEnabled()
        && isLevelEnabled(HoarderProperties.LogLevel.INFO)
        && logger.isInfoEnabled()) {
      logger.info("[{}] {}", formatArgs(clazz, args), message);
    }
  }

  public void debug(Class<?> clazz, String message, Object... args) {
    if (isLoggingEnabled()
        && isLevelEnabled(HoarderProperties.LogLevel.DEBUG)
        && logger.isDebugEnabled()) {
      logger.debug("[{}] {}", formatArgs(clazz, args), message);
    }
  }

  public void trace(Class<?> clazz, String message, Object... args) {
    if (isLoggingEnabled()
        && isLevelEnabled(HoarderProperties.LogLevel.TRACE)
        && logger.isTraceEnabled()) {
      logger.trace("[{}] {}", formatArgs(clazz, args), message);
    }
  }

  public void warn(Class<?> clazz, String message, Object... args) {
    if (isLoggingEnabled()
        && isLevelEnabled(HoarderProperties.LogLevel.WARN)
        && logger.isWarnEnabled()) {
      logger.warn("[{}] {}", formatArgs(clazz, args), message);
    }
  }

  public void error(Class<?> clazz, String message, Object... args) {
    if (isLoggingEnabled()
        && isLevelEnabled(HoarderProperties.LogLevel.ERROR)
        && logger.isErrorEnabled()) {
      logger.error("[{}] {}", formatArgs(clazz, args), message);
    }
  }

  private boolean isLoggingEnabled() {
    return hoarderProperties.getLogging().isEnabled();
  }

  private boolean isLevelEnabled(HoarderProperties.LogLevel level) {
    return level.ordinal() >= hoarderProperties.getLogging().getLevel().ordinal();
  }

  private Object[] formatArgs(Class<?> clazz, Object... args) {
    Object[] newArgs = new Object[args.length + 1];
    newArgs[0] = clazz.getSimpleName();
    System.arraycopy(args, 0, newArgs, 1, args.length);
    return newArgs;
  }
}
