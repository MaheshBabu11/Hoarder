package com.maheshbabu11.hoarder.core;

import com.maheshbabu11.hoarder.annotation.Hoarded;
import com.maheshbabu11.hoarder.annotation.HoardedColumn;
import com.maheshbabu11.hoarder.config.HoarderProperties;
import com.maheshbabu11.hoarder.util.HoarderLogger;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(value = "hoarder.cache.enabled", havingValue = "true", matchIfMissing = true)
public class HoarderInitializer {

  private final EntityManager entityManager;
  private final HoarderCache hoarderCache;
  private final HoarderProperties hoarderProperties;
  private final HoarderLogger hoarderLogger;

  public HoarderInitializer(
      EntityManager entityManager,
      HoarderCache hoarderCache,
      HoarderProperties hoarderProperties,
      HoarderLogger hoarderLogger) {
    this.entityManager = entityManager;
    this.hoarderCache = hoarderCache;
    this.hoarderProperties = hoarderProperties;
    this.hoarderLogger = hoarderLogger;
  }

  @PostConstruct
  public void initializeCache() {
    if (!hoarderProperties.getCache().isEnabled()) {
      hoarderLogger.info(
          HoarderInitializer.class, "Hoarder cache is disabled, skipping initialization.");
      return;
    }

    hoarderLogger.info(HoarderInitializer.class, "Starting Hoarder cache initialization...");

    Set<EntityType<?>> entityTypes = entityManager.getMetamodel().getEntities();
    int totalEntitiesProcessed = 0;
    int totalRecordsCached = 0;

    for (EntityType<?> entityType : entityTypes) {
      Class<?> entityClass = entityType.getJavaType();

      if (entityClass.isAnnotationPresent(Hoarded.class)) {
        try {
          int recordsProcessed = loadAndCacheEntity(entityClass);
          totalEntitiesProcessed++;
          totalRecordsCached += recordsProcessed;

          hoarderLogger.info(
              HoarderInitializer.class,
              "Loaded {} records for entity: {}",
              recordsProcessed,
              entityClass.getSimpleName());

        } catch (Exception e) {
          hoarderLogger.error(
              HoarderInitializer.class,
              "Failed to load entity {}: {}",
              entityClass.getSimpleName(),
              e.getMessage());
        }
      }
    }

    hoarderLogger.info(
        HoarderInitializer.class,
        "Hoarder cache initialization completed. Processed {} entities with {} total records.",
        totalEntitiesProcessed,
        totalRecordsCached);
  }

  private int loadAndCacheEntity(Class<?> entityClass) {
    String entityName = entityClass.getSimpleName();

    // Single query to fetch all records
    String jpql = "SELECT e FROM " + entityName + " e";
    Query query = entityManager.createQuery(jpql, entityClass);
    List<?> records = query.getResultList();

    if (records.isEmpty()) {
      hoarderLogger.debug(HoarderInitializer.class, "No records found for entity: {}", entityName);
      return 0;
    }

    // Cache by ID (primary key)
    cacheById(entityClass, records);

    // Cache by all available columns
    cacheByColumns(entityClass, records);

    return records.size();
  }

  private void cacheById(Class<?> entityClass, List<?> records) {
    try {
      // Find the ID field/method
      Method idGetter = findIdGetter(entityClass);

      if (idGetter != null) {
        hoarderCache.preload(
            entityClass,
            records,
            record -> {
              try {
                return idGetter.invoke(record);
              } catch (Exception e) {
                hoarderLogger.warn(
                    HoarderInitializer.class,
                    "Failed to extract ID from entity {}: {}",
                    entityClass.getSimpleName(),
                    e.getMessage());
                return null;
              }
            });

        hoarderLogger.debug(
            HoarderInitializer.class,
            "Cached {} records by ID for entity: {}",
            records.size(),
            entityClass.getSimpleName());
      }
    } catch (Exception e) {
      hoarderLogger.warn(
          HoarderInitializer.class,
          "Failed to cache by ID for entity {}: {}",
          entityClass.getSimpleName(),
          e.getMessage());
    }
  }

  private void cacheByColumns(Class<?> entityClass, List<?> records) {
    Field[] fields = entityClass.getDeclaredFields();
    int columnsCached = 0;

    for (Field field : fields) {
      try {
        String fieldName = field.getName();
        // Skip ID field as it's already cached
        if (isIdField(field)) {
          continue;
        }

        // Only cache fields annotated with @HoardedColumn
        if (!field.isAnnotationPresent(HoardedColumn.class)) {
          continue;
        }

        String getterName = "get" + capitalize(fieldName);
        Method getter = entityClass.getMethod(getterName);

        hoarderCache.preloadByColumn(
            entityClass,
            fieldName,
            records,
            record -> {
              try {
                return getter.invoke(record);
              } catch (Exception e) {
                hoarderLogger.trace(
                    HoarderInitializer.class,
                    "Failed to extract value for field {} from entity {}: {}",
                    fieldName,
                    entityClass.getSimpleName(),
                    e.getMessage());
                return null;
              }
            });

        columnsCached++;
        hoarderLogger.trace(
            HoarderInitializer.class,
            "Cached {} records by column '{}' for entity: {}",
            records.size(),
            fieldName,
            entityClass.getSimpleName());

      } catch (NoSuchMethodException e) {
        hoarderLogger.trace(
            HoarderInitializer.class,
            "No getter found for field '{}' in entity: {}",
            field.getName(),
            entityClass.getSimpleName());
      } catch (Exception e) {
        hoarderLogger.warn(
            HoarderInitializer.class,
            "Failed to cache by column '{}' for entity {}: {}",
            field.getName(),
            entityClass.getSimpleName(),
            e.getMessage());
      }
    }

    hoarderLogger.debug(
        HoarderInitializer.class,
        "Cached {} columns for entity: {}",
        columnsCached,
        entityClass.getSimpleName());
  }

  private Method findIdGetter(Class<?> entityClass) {
    // Try to find @Id annotated field and its getter
    for (Field field : entityClass.getDeclaredFields()) {
      if (isIdField(field)) {
        try {
          String getterName = "get" + capitalize(field.getName());
          return entityClass.getMethod(getterName);
        } catch (NoSuchMethodException e) {
          hoarderLogger.debug(
              HoarderInitializer.class,
              "No getter found for ID field '{}' in entity: {}",
              field.getName(),
              entityClass.getSimpleName());
        }
      }
    }
    return null;
  }

  private boolean isIdField(Field field) {
    return field.isAnnotationPresent(jakarta.persistence.Id.class)
        || field.isAnnotationPresent(jakarta.persistence.EmbeddedId.class);
  }

  private String capitalize(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1);
  }
}
