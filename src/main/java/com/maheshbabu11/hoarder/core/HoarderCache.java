package com.maheshbabu11.hoarder.core;

import com.maheshbabu11.hoarder.config.HoarderProperties;
import com.maheshbabu11.hoarder.util.HoarderLogger;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Component
public class HoarderCache {
  private static final Map<Class<?>, Map<Object, Object>> CACHE = new ConcurrentHashMap<>();
  private final HoarderProperties properties;
  private final HoarderLogger hoarderLogger;

  public HoarderCache(HoarderProperties properties, HoarderLogger hoarderLogger) {
    this.properties = properties;
    this.hoarderLogger = hoarderLogger;
  }

  public <T> void preload(Class<?> clazz, List<?> records, Function<Object, Object> idExtractor) {
    if (!properties.getCache().isEnabled()) return;

    Map<Object, Object> entityMap = CACHE.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());
    for (Object record : records) {
      entityMap.put(idExtractor.apply(record), record);
    }
  }

  @SuppressWarnings("unchecked")
  public <T> Optional<T> get(Class<T> clazz, Object id) {
    if (!properties.getCache().isEnabled()) return Optional.empty();
    return Optional.ofNullable((T) CACHE.getOrDefault(clazz, Collections.emptyMap()).get(id));
  }

  public <T> void put(Class<?> clazz, Object id, T entity) {
    if (!properties.getCache().isEnabled()) return;
    CACHE.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>()).put(id, entity);
  }

  public boolean isCached(Class<?> clazz) {
    return properties.getCache().isEnabled() && CACHE.containsKey(clazz);
  }

  public void clear() {
    CACHE.clear();
  }

  public void printCacheStatus() {
    hoarderLogger.info(
        HoarderCache.class,
        "Current Hoarder Cache Status (Enabled: {}):",
        properties.getCache().isEnabled());
    if (CACHE.isEmpty()) {
      hoarderLogger.info(HoarderCache.class, "Cache is empty.");
      return;
    }
    for (Map.Entry<Class<?>, Map<Object, Object>> entry : CACHE.entrySet()) {
      Class<?> clazz = entry.getKey();
      Map<Object, Object> entityMap = entry.getValue();
      hoarderLogger.info(
          HoarderCache.class,
          "Class: {}, Cached Entities: {}",
          clazz.getSimpleName(),
          entityMap.size());
    }
  }

  public void printCacheDetails() {
    hoarderLogger.info(
        HoarderCache.class,
        "Hoarder Cache Details (Enabled: {}):",
        properties.getCache().isEnabled());
    for (Map.Entry<Class<?>, Map<Object, Object>> entry : CACHE.entrySet()) {
      Class<?> clazz = entry.getKey();
      Map<Object, Object> entityMap = entry.getValue();
      hoarderLogger.info(
          HoarderCache.class,
          "Class: {}, Cached Entities: {}",
          clazz.getSimpleName(),
          entityMap.size());
      for (Map.Entry<Object, Object> entityEntry : entityMap.entrySet()) {
        hoarderLogger.debug(
            HoarderCache.class,
            "  ID: {}, Entity: {}",
            entityEntry.getKey(),
            entityEntry.getValue());
      }
    }
  }

}
