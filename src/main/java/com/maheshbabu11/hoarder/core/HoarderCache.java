package com.maheshbabu11.hoarder.core;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class HoarderCache {
    private static final Map<Class<?>, Map<Object, Object>> CACHE = new ConcurrentHashMap<>();

    public static <T> void preload(Class<?> clazz, List<?> records, Function<Object, Object> idExtractor) {
        Map<Object, Object> entityMap = CACHE.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());
        for (Object record : records) {
            entityMap.put(idExtractor.apply(record), record);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> get(Class<T> clazz, Object id) {
        return Optional.ofNullable((T) CACHE.getOrDefault(clazz, Collections.emptyMap()).get(id));
    }

    public static <T> void put(Class<?> clazz, Object id, T entity) {
        CACHE.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>()).put(id, entity);
    }

    public static boolean isCached(Class<?> clazz) {
        return CACHE.containsKey(clazz);
    }
}

