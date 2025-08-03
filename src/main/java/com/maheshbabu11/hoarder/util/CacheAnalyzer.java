package com.maheshbabu11.hoarder.util;

import com.maheshbabu11.hoarder.core.HoarderCache;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

@Component
public class CacheAnalyzer {

  private final HoarderLogger hoarderLogger;

  public CacheAnalyzer(HoarderLogger hoarderLogger) {
    this.hoarderLogger = hoarderLogger;
  }

  public void printCacheSizes() {
    try {
      // Access the static cache maps using reflection
      Field cacheField = HoarderCache.class.getDeclaredField("CACHE");
      Field columnCacheField = HoarderCache.class.getDeclaredField("COLUMN_CACHE");

      cacheField.setAccessible(true);
      columnCacheField.setAccessible(true);

      @SuppressWarnings("unchecked")
      Map<Class<?>, Map<Object, Object>> cache =
          (Map<Class<?>, Map<Object, Object>>) cacheField.get(null);

      @SuppressWarnings("unchecked")
      Map<Class<?>, Map<String, Map<Object, List<Object>>>> columnCache =
          (Map<Class<?>, Map<String, Map<Object, List<Object>>>>) columnCacheField.get(null);

      // Calculate and print main cache sizes
      hoarderLogger.info(CacheAnalyzer.class, "=== Hoarder Cache Size Analysis ===");

      int totalMainCacheEntries = 0;
      for (Map.Entry<Class<?>, Map<Object, Object>> entry : cache.entrySet()) {
        int entityCount = entry.getValue().size();
        totalMainCacheEntries += entityCount;

        hoarderLogger.info(
            CacheAnalyzer.class,
            "Main Cache - Class: {}, Entities: {}",
            entry.getKey().getSimpleName(),
            entityCount);
      }

      // Calculate and print column cache sizes
      int totalColumnCacheEntries = 0;
      int totalColumnCacheValues = 0;

      for (Map.Entry<Class<?>, Map<String, Map<Object, List<Object>>>> classEntry :
          columnCache.entrySet()) {
        String className = classEntry.getKey().getSimpleName();

        for (Map.Entry<String, Map<Object, List<Object>>> columnEntry :
            classEntry.getValue().entrySet()) {
          String columnName = columnEntry.getKey();
          Map<Object, List<Object>> columnMap = columnEntry.getValue();

          int distinctValues = columnMap.size();
          int totalEntities = columnMap.values().stream().mapToInt(List::size).sum();

          totalColumnCacheEntries += distinctValues;
          totalColumnCacheValues += totalEntities;

          hoarderLogger.info(
              CacheAnalyzer.class,
              "Column Cache - Class: {}, Column: {}, Distinct Values: {}, Total Entities: {}",
              className,
              columnName,
              distinctValues,
              totalEntities);
        }
      }

      // Print summary
      hoarderLogger.info(CacheAnalyzer.class, "=== Cache Summary ===");
      hoarderLogger.info(
          CacheAnalyzer.class, "Main Cache Total Entities: {}", totalMainCacheEntries);
      hoarderLogger.info(
          CacheAnalyzer.class, "Column Cache Total Distinct Values: {}", totalColumnCacheEntries);
      hoarderLogger.info(
          CacheAnalyzer.class, "Column Cache Total Entity References: {}", totalColumnCacheValues);
      hoarderLogger.info(CacheAnalyzer.class, "Total Cache Classes: {}", cache.size());
      hoarderLogger.info(CacheAnalyzer.class, "Total Column Cache Classes: {}", columnCache.size());

    } catch (Exception e) {
      hoarderLogger.error(CacheAnalyzer.class, "Failed to analyze cache sizes: {}", e.getMessage());
    }
  }
}
