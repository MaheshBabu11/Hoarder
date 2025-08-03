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
      logger.info("[{}] " + message, formatArgs(clazz, args));
    }
  }

  public void debug(Class<?> clazz, String message, Object... args) {
    if (isLoggingEnabled()
            && isLevelEnabled(HoarderProperties.LogLevel.DEBUG)
            && logger.isDebugEnabled()) {
      logger.debug("[{}] " + message, formatArgs(clazz, args));
    }
  }

  public void trace(Class<?> clazz, String message, Object... args) {
    if (isLoggingEnabled()
            && isLevelEnabled(HoarderProperties.LogLevel.TRACE)
            && logger.isTraceEnabled()) {
      logger.trace("[{}] " + message, formatArgs(clazz, args));
    }
  }

  public void warn(Class<?> clazz, String message, Object... args) {
    if (isLoggingEnabled()
            && isLevelEnabled(HoarderProperties.LogLevel.WARN)
            && logger.isWarnEnabled()) {
      logger.warn("[{}] " + message, formatArgs(clazz, args));
    }
  }

  public void error(Class<?> clazz, String message, Object... args) {
    if (isLoggingEnabled()
            && isLevelEnabled(HoarderProperties.LogLevel.ERROR)
            && logger.isErrorEnabled()) {
      logger.error("[{}] " + message, formatArgs(clazz, args));
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