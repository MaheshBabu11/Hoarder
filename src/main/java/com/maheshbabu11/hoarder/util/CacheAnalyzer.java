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
      long totalMainCacheMemory = 0;

      for (Map.Entry<Class<?>, Map<Object, Object>> entry : cache.entrySet()) {
        int entityCount = entry.getValue().size();
        long entityMemory = calculateMapMemorySize(entry.getValue());

        totalMainCacheEntries += entityCount;
        totalMainCacheMemory += entityMemory;

        hoarderLogger.info(
                CacheAnalyzer.class,
                "Main Cache - Class: {}, Entities: {}, Memory: {} bytes ({} MB)",
                entry.getKey().getSimpleName(),
                entityCount,
                entityMemory,
                formatMB(entityMemory));
      }

      // Calculate and print column cache sizes
      int totalColumnCacheEntries = 0;
      int totalColumnCacheValues = 0;
      long totalColumnCacheMemory = 0;

      for (Map.Entry<Class<?>, Map<String, Map<Object, List<Object>>>> classEntry :
              columnCache.entrySet()) {
        String className = classEntry.getKey().getSimpleName();
        long classColumnMemory = calculateNestedMapMemorySize(classEntry.getValue());

        for (Map.Entry<String, Map<Object, List<Object>>> columnEntry :
                classEntry.getValue().entrySet()) {
          String columnName = columnEntry.getKey();
          Map<Object, List<Object>> columnMap = columnEntry.getValue();

          int distinctValues = columnMap.size();
          int totalEntities = columnMap.values().stream().mapToInt(List::size).sum();
          long columnMemory = calculateColumnMapMemorySize(columnMap);

          totalColumnCacheEntries += distinctValues;
          totalColumnCacheValues += totalEntities;

          hoarderLogger.info(
                  CacheAnalyzer.class,
                  "Column Cache - Class: {}, Column: {}, Distinct Values: {}, Total Entities: {}, Memory: {} bytes ({} MB)",
                  className,
                  columnName,
                  distinctValues,
                  totalEntities,
                  columnMemory,
                  formatMB(columnMemory));
        }

        totalColumnCacheMemory += classColumnMemory;
      }

      long totalMemory = totalMainCacheMemory + totalColumnCacheMemory;

      // Print summary
      hoarderLogger.info(CacheAnalyzer.class, "=== Cache Summary ===");
      hoarderLogger.info(
              CacheAnalyzer.class, "Main Cache Total Entities: {}", totalMainCacheEntries);
      hoarderLogger.info(
              CacheAnalyzer.class, "Main Cache Total Memory: {} bytes ({} MB)", totalMainCacheMemory, formatMB(totalMainCacheMemory));
      hoarderLogger.info(
              CacheAnalyzer.class, "Column Cache Total Distinct Values: {}", totalColumnCacheEntries);
      hoarderLogger.info(
              CacheAnalyzer.class, "Column Cache Total Entity References: {}", totalColumnCacheValues);
      hoarderLogger.info(
              CacheAnalyzer.class, "Column Cache Total Memory: {} bytes ({} MB)", totalColumnCacheMemory, formatMB(totalColumnCacheMemory));
      hoarderLogger.info(CacheAnalyzer.class, "Total Cache Classes: {}", cache.size());
      hoarderLogger.info(CacheAnalyzer.class, "Total Column Cache Classes: {}", columnCache.size());
      hoarderLogger.info(CacheAnalyzer.class, "TOTAL CACHE MEMORY: {} bytes ({} MB)", totalMemory, formatMB(totalMemory));

    } catch (Exception e) {
      hoarderLogger.error(CacheAnalyzer.class, "Failed to analyze cache sizes: {}", e.getMessage());
    }
  }

  private long calculateMapMemorySize(Map<Object, Object> map) {
    long size = 0;
    // Base HashMap overhead (approximate)
    size += 32; // HashMap object overhead
    size += map.size() * 32L; // Entry objects overhead

    for (Map.Entry<Object, Object> entry : map.entrySet()) {
      size += estimateObjectSize(entry.getKey());
      size += estimateObjectSize(entry.getValue());
    }
    return size;
  }

  private long calculateNestedMapMemorySize(Map<String, Map<Object, List<Object>>> nestedMap) {
    long size = 32; // Base HashMap overhead
    size += nestedMap.size() * 32L; // Entry objects

    for (Map.Entry<String, Map<Object, List<Object>>> entry : nestedMap.entrySet()) {
      size += estimateObjectSize(entry.getKey());
      size += calculateColumnMapMemorySize(entry.getValue());
    }
    return size;
  }

  private long calculateColumnMapMemorySize(Map<Object, List<Object>> columnMap) {
    long size = 32; // Base HashMap overhead
    size += columnMap.size() * 32L; // Entry objects

    for (Map.Entry<Object, List<Object>> entry : columnMap.entrySet()) {
      size += estimateObjectSize(entry.getKey());
      // List overhead + references
      size += 24; // ArrayList overhead
      size += entry.getValue().size() * 8L; // Object references
    }
    return size;
  }

  private long estimateObjectSize(Object obj) {
    if (obj == null) return 0;
    if (obj instanceof String) {
      return 40 + ((String) obj).length() * 2L; // String overhead + char array
    }
    if (obj instanceof Integer) return 16;
    if (obj instanceof Long) return 24;
    if (obj instanceof Double) return 24;
    if (obj instanceof Boolean) return 16;
    // For other objects, assume a base size (this is approximate)
    return 48; // Estimated object overhead
  }

  private String formatMB(long bytes) {
    return String.format("%.2f", bytes / (1024.0 * 1024.0));
  }
}