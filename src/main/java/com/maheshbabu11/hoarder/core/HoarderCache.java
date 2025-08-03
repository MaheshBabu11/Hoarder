package com.maheshbabu11.hoarder.core;

import com.maheshbabu11.hoarder.config.HoarderProperties;
import com.maheshbabu11.hoarder.util.CacheAnalyzer;
import com.maheshbabu11.hoarder.util.HoarderLogger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(value = "hoarder.cache.enabled", havingValue = "true", matchIfMissing = true)
public class HoarderCache {
  private static final Map<Class<?>, Map<Object, Object>> CACHE = new ConcurrentHashMap<>();
  private static final Map<Class<?>, Map<String, Map<Object, List<Object>>>> COLUMN_CACHE =
      new ConcurrentHashMap<>();

  private final HoarderProperties properties;
  private final HoarderLogger hoarderLogger;
  private final CacheAnalyzer cacheAnalyzer;

  public HoarderCache(
      HoarderProperties properties, HoarderLogger hoarderLogger, CacheAnalyzer cacheAnalyzer) {
    this.properties = properties;
    this.hoarderLogger = hoarderLogger;
    this.cacheAnalyzer = cacheAnalyzer;
  }

  public void preload(Class<?> clazz, List<?> records, Function<Object, Object> idExtractor) {
    if (records.isEmpty()) return;

    Map<Object, Object> entityMap = CACHE.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());
    records.parallelStream()
        .forEach(
            record -> {
              Object id = idExtractor.apply(record);
              if (id != null) {
                entityMap.put(id, record);
              }
            });
  }

  public void preloadByColumn(
      Class<?> clazz,
      String columnName,
      List<?> records,
      Function<Object, Object> columnExtractor) {
    if (records.isEmpty()) return;

    Map<String, Map<Object, List<Object>>> classColumnCache =
        COLUMN_CACHE.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());
    Map<Object, List<Object>> columnMap =
        classColumnCache.computeIfAbsent(columnName, k -> new ConcurrentHashMap<>());

    // Group records by column value for better handling of duplicate values
    Map<Object, List<Object>> groupedRecords =
        records.parallelStream()
            .collect(
                Collectors.groupingBy(
                    record -> {
                      Object value = columnExtractor.apply(record);
                      return value != null ? value : "NULL_VALUE";
                    },
                    Collectors.toList()));

    groupedRecords.forEach(
        (value, recordList) -> {
          if (!"NULL_VALUE".equals(value)) {
            columnMap.put(value, recordList);
          }
        });
  }

  @SuppressWarnings("unchecked")
  public <T> Optional<T> get(Class<T> clazz, Object id) {
    if (id == null) return Optional.empty();

    return Optional.ofNullable((T) CACHE.getOrDefault(clazz, Collections.emptyMap()).get(id));
  }

  @SuppressWarnings("unchecked")
  public <T> Optional<T> getByColumn(Class<T> clazz, String columnName, Object value) {
    if (value == null) return Optional.empty();

    return getColumnMap(clazz, columnName)
        .map(columnMap -> columnMap.get(value))
        .filter(Objects::nonNull)
        .filter(list -> !list.isEmpty())
        .map(list -> (T) list.get(0));
  }

  @SuppressWarnings("unchecked")
  public <T> List<T> getAllByColumn(Class<T> clazz, String columnName, Object value) {
    if (value == null) return Collections.emptyList();

    return getColumnMap(clazz, columnName)
        .map(columnMap -> columnMap.get(value))
        .map(list -> list.stream().map(obj -> (T) obj).collect(Collectors.toList()))
        .orElse(Collections.emptyList());
  }

  public <T> void put(Class<?> clazz, Object id, T entity) {
    if (id == null || entity == null) return;

    CACHE.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>()).put(id, entity);
  }

  public <T> void putByColumn(Class<?> clazz, String columnName, Object value, T entity) {
    if (value == null || entity == null) return;

    Map<String, Map<Object, List<Object>>> classColumnCache =
        COLUMN_CACHE.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());
    Map<Object, List<Object>> columnMap =
        classColumnCache.computeIfAbsent(columnName, k -> new ConcurrentHashMap<>());

    columnMap.computeIfAbsent(value, k -> new ArrayList<>()).add(entity);
  }

  public boolean isCached(Class<?> clazz) {
    return isCacheEnabled() && CACHE.containsKey(clazz);
  }

  public boolean isColumnCached(Class<?> clazz, String columnName) {

    return Optional.ofNullable(COLUMN_CACHE.get(clazz))
        .map(classCache -> classCache.containsKey(columnName))
        .orElse(false);
  }

  public void clear() {
    CACHE.clear();
    COLUMN_CACHE.clear();
    hoarderLogger.info(HoarderCache.class, "Cleared all cached entities");
  }

  public void clearForEntity(Class<?> clazz) {
    Map<Object, Object> entityMap = CACHE.remove(clazz);
    Map<String, Map<Object, List<Object>>> columnMap = COLUMN_CACHE.remove(clazz);

    int clearedEntities = entityMap != null ? entityMap.size() : 0;
    int clearedColumns = columnMap != null ? columnMap.size() : 0;

    hoarderLogger.debug(
        HoarderCache.class,
        "Cleared {} cached entities and {} column caches for class: {}",
        clearedEntities,
        clearedColumns,
        clazz.getSimpleName());
  }

  public void printCacheStatus() {
    hoarderLogger.info(
        HoarderCache.class, "Current Hoarder Cache Status (Enabled: {}):", isCacheEnabled());

    if (CACHE.isEmpty()) {
      hoarderLogger.info(HoarderCache.class, "Cache is empty.");
      return;
    }

    CACHE.forEach(
        (clazz, entityMap) ->
            hoarderLogger.info(
                HoarderCache.class,
                "Class: {}, Cached Entities: {}",
                clazz.getSimpleName(),
                entityMap.size()));

    COLUMN_CACHE.forEach(
        (clazz, columnMaps) ->
            columnMaps.forEach(
                (columnName, columnMap) ->
                    hoarderLogger.info(
                        HoarderCache.class,
                        "Class: {}, Column: {}, Cached Values: {}",
                        clazz.getSimpleName(),
                        columnName,
                        columnMap.size())));
  }

  public void printCacheDetails() {
    hoarderLogger.info(
        HoarderCache.class, "Hoarder Cache Details (Enabled: {}):", isCacheEnabled());

    CACHE.forEach(
        (clazz, entityMap) -> {
          hoarderLogger.info(
              HoarderCache.class,
              "Class: {}, Cached Entities: {}",
              clazz.getSimpleName(),
              entityMap.size());

          entityMap.forEach(
              (id, entity) ->
                  hoarderLogger.debug(HoarderCache.class, "  ID: {}, Entity: {}", id, entity));
        });

    COLUMN_CACHE.forEach(
        (clazz, columnMaps) ->
            columnMaps.forEach(
                (columnName, columnMap) -> {
                  hoarderLogger.info(
                      HoarderCache.class,
                      "Class: {}, Column: {}, Cached Values: {}",
                      clazz.getSimpleName(),
                      columnName,
                      columnMap.size());

                  columnMap.forEach(
                      (value, entities) ->
                          hoarderLogger.debug(
                              HoarderCache.class, "    {}: {} entities", value, entities.size()));
                }));
  }

  // Helper methods
  private boolean isCacheEnabled() {
    return properties.getCache().isEnabled();
  }

  private Optional<Map<Object, List<Object>>> getColumnMap(Class<?> clazz, String columnName) {
    return Optional.ofNullable(COLUMN_CACHE.get(clazz))
        .map(classCache -> classCache.get(columnName));
  }

  public void getCacheSize() {
    cacheAnalyzer.printCacheSizes();
    hoarderLogger.info(HoarderCache.class, "Cache size analysis completed.");
  }
}
