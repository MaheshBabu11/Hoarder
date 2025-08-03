package com.maheshbabu11.hoarder.core;

import com.maheshbabu11.hoarder.annotation.Hoarded;
import com.maheshbabu11.hoarder.config.HoarderProperties;
import com.maheshbabu11.hoarder.util.HoarderLogger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;

@Component
public class HoarderInitializer {

  private final EntityManager entityManager;
  private final HoarderProperties hoarderProperties;
  private final HoarderLogger hoarderLogger;
  private final HoarderCache hoarderCache;
  private final Map<Class<?>, Method> idGetterCache = new ConcurrentHashMap<>();
  private ScheduledExecutorService refreshScheduler;

  public HoarderInitializer(
      EntityManager entityManager,
      HoarderProperties hoarderProperties,
      HoarderLogger hoarderLogger,
      HoarderCache hoarderCache) {
    this.entityManager = entityManager;
    this.hoarderProperties = hoarderProperties;
    this.hoarderLogger = hoarderLogger;
    this.hoarderCache = hoarderCache;
  }

  @PostConstruct
  public void init() {
    if (!hoarderProperties.getCache().isEnabled()) {
      hoarderLogger.info(
          HoarderInitializer.class, "Hoarder cache is disabled, skipping entity preloading.");
      return;
    }

    Set<EntityType<?>> entityTypes = entityManager.getMetamodel().getEntities();

    for (EntityType<?> entityType : entityTypes) {
      Class<?> javaType = entityType.getJavaType();

      if (javaType.isAnnotationPresent(Hoarded.class)) {
        hoarderLogger.info(
            HoarderInitializer.class, "Found hoarded entity: {}", javaType.getSimpleName());
        preloadEntity(javaType);
      }
    }

    if (hoarderProperties.getCache().getRefresh().isEnabled()) {
      startCacheRefreshScheduler();
    }
  }

  @PreDestroy
  public void destroy() {
    if (refreshScheduler != null && !refreshScheduler.isShutdown()) {
      hoarderLogger.info(HoarderInitializer.class, "Shutting down cache refresh scheduler");
      refreshScheduler.shutdown();
      try {
        if (!refreshScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
          refreshScheduler.shutdownNow();
        }
      } catch (InterruptedException e) {
        refreshScheduler.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  private void startCacheRefreshScheduler() {
    refreshScheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "hoarder-cache-refresh");
              t.setDaemon(true);
              return t;
            });

    long delayMinutes = hoarderProperties.getCache().getRefresh().getDelayMinutes();
    long intervalMinutes = hoarderProperties.getCache().getRefresh().getIntervalMinutes();

    hoarderLogger.info(
        HoarderInitializer.class,
        "Starting cache refresh scheduler with delay: {} minutes, interval: {} minutes",
        delayMinutes,
        intervalMinutes);

    refreshScheduler.scheduleAtFixedRate(
        this::refreshAllCachedEntities, delayMinutes, intervalMinutes, TimeUnit.MINUTES);
  }

  private void refreshAllCachedEntities() {
    hoarderLogger.info(HoarderInitializer.class, "Starting scheduled cache refresh");

    try {
      Set<EntityType<?>> entityTypes = entityManager.getMetamodel().getEntities();

      for (EntityType<?> entityType : entityTypes) {
        Class<?> javaType = entityType.getJavaType();

        if (javaType.isAnnotationPresent(Hoarded.class) && hoarderCache.isCached(javaType)) {
          hoarderLogger.debug(
              HoarderInitializer.class,
              "Refreshing cached entities for: {}",
              javaType.getSimpleName());
          refreshEntity(javaType);
        }
      }

      hoarderLogger.info(HoarderInitializer.class, "Completed scheduled cache refresh");
    } catch (Exception e) {
      hoarderLogger.error(
          HoarderInitializer.class, "Error during scheduled cache refresh: {}", e.getMessage());
    }
  }

  private void refreshEntity(Class<?> clazz) {
    try {
      String jpql = "SELECT e FROM " + clazz.getSimpleName() + " e";
      List<?> result = entityManager.createQuery(jpql, clazz).getResultList();

      Method idGetter = idGetterCache.computeIfAbsent(clazz, this::findIdGetter);

      if (idGetter == null) {
        hoarderLogger.error(
            HoarderInitializer.class,
            "ID getter not found for class during refresh: {}",
            clazz.getSimpleName());
        return;
      }

      hoarderCache.clearForEntity(clazz);

      hoarderCache.preload(
          clazz,
          result,
          entity -> {
            try {
              return idGetter.invoke(entity);
            } catch (Exception e) {
              throw new RuntimeException(
                  "Failed to invoke ID getter for entity during refresh: " + clazz.getSimpleName(),
                  e);
            }
          });

      hoarderLogger.debug(
          HoarderInitializer.class,
          "Successfully refreshed {} entities for class: {}",
          result.size(),
          clazz.getSimpleName());

    } catch (Exception e) {
      hoarderLogger.error(
          HoarderInitializer.class,
          "Failed to refresh entities for class: {}, Error: {}",
          clazz.getSimpleName(),
          e.getMessage());
    }
  }

  private void preloadEntity(Class<?> clazz) {
    try {
      String jpql = "SELECT e FROM " + clazz.getSimpleName() + " e";
      hoarderLogger.debug(
          HoarderInitializer.class, "Preloading entities for class: {}", clazz.getSimpleName());

      List<?> result = entityManager.createQuery(jpql, clazz).getResultList();
      Method idGetter = idGetterCache.computeIfAbsent(clazz, this::findIdGetter);

      if (idGetter == null) {
        hoarderLogger.error(
            HoarderInitializer.class, "ID getter not found for class: {}", clazz.getSimpleName());
        return;
      }

      hoarderCache.preload(
          clazz,
          result,
          entity -> {
            try {
              return idGetter.invoke(entity);
            } catch (Exception e) {
              throw new RuntimeException(
                  "Failed to invoke ID getter for entity: " + clazz.getSimpleName(), e);
            }
          });

      hoarderLogger.info(
          HoarderInitializer.class,
          "Successfully preloaded {} entities for class: {}",
          result.size(),
          clazz.getSimpleName());

    } catch (Exception e) {
      hoarderLogger.error(
          HoarderInitializer.class,
          "Failed to preload entities for class: {}, Error: {}",
          clazz.getSimpleName(),
          e.getMessage());
    }
  }

  private Method findIdGetter(Class<?> clazz) {
    hoarderLogger.trace(
        HoarderInitializer.class, "Finding ID getter for class: {}", clazz.getSimpleName());

    // First, check methods for @Id annotation (method-level JPA annotation)
    for (Method method : clazz.getMethods()) {
      if (method.isAnnotationPresent(Id.class)) {
        hoarderLogger.trace(
            HoarderInitializer.class,
            "Found ID getter method: {} for class: {}",
            method.getName(),
            clazz.getSimpleName());
        return method;
      }
    }

    // Then, check fields for @Id annotation and find corresponding getter
    for (Field field : clazz.getDeclaredFields()) {
      if (field.isAnnotationPresent(Id.class)) {
        String fieldName = field.getName();
        String getterName =
            "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        try {
          Method getter = clazz.getMethod(getterName);
          hoarderLogger.trace(
              HoarderInitializer.class,
              "Found ID getter method: {} for field: {} in class: {}",
              getterName,
              fieldName,
              clazz.getSimpleName());
          return getter;
        } catch (NoSuchMethodException e) {
          // Try boolean getter pattern (isXxx for boolean fields)
          if (field.getType() == boolean.class || field.getType() == Boolean.class) {
            String booleanGetterName =
                "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            try {
              Method booleanGetter = clazz.getMethod(booleanGetterName);
              hoarderLogger.trace(
                  HoarderInitializer.class,
                  "Found boolean ID getter method: {} for field: {} in class: {}",
                  booleanGetterName,
                  fieldName,
                  clazz.getSimpleName());
              return booleanGetter;
            } catch (NoSuchMethodException ignored) {
              // Continue to next field
            }
          }
        }
      }
    }

    hoarderLogger.warn(
        HoarderInitializer.class, "No ID getter found for class: {}", clazz.getSimpleName());
    return null;
  }
}
