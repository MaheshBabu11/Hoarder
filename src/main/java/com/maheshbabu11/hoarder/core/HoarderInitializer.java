package com.maheshbabu11.hoarder.core;

import com.maheshbabu11.hoarder.annotation.Hoarded;
import com.maheshbabu11.hoarder.config.HoarderProperties;
import com.maheshbabu11.hoarder.util.HoarderLogger;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

@Component
public class HoarderInitializer {

  private final EntityManager entityManager;
  private final HoarderProperties hoarderProperties;
  private final HoarderLogger hoarderLogger;
  private final HoarderCache hoarderCache;

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
  }

  private void preloadEntity(Class<?> clazz) {
    try {
      String jpql = "SELECT e FROM " + clazz.getSimpleName() + " e";
      hoarderLogger.debug(
          HoarderInitializer.class, "Preloading entities for class: {}", clazz.getSimpleName());

      List<?> result = entityManager.createQuery(jpql, clazz).getResultList();
      Method idGetter = findIdGetter(clazz);

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
