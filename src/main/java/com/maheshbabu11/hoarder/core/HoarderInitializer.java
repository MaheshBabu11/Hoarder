package com.maheshbabu11.hoarder.core;

import com.maheshbabu11.hoarder.annotation.Hoarded;
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

    public HoarderInitializer(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @PostConstruct
    public void init() {
        // Get all JPA entities from the EntityManager's metamodel
        Set<EntityType<?>> entityTypes = entityManager.getMetamodel().getEntities();

        for (EntityType<?> entityType : entityTypes) {
            Class<?> javaType = entityType.getJavaType();

            // Check if this entity is annotated with @Hoarded
            if (javaType.isAnnotationPresent(Hoarded.class)) {
                System.out.println("Found hoarded entity: " + javaType.getSimpleName());
                preloadEntity(javaType);
            }
        }
    }

    private void preloadEntity(Class<?> clazz) {
        try {
            String jpql = "SELECT e FROM " + clazz.getSimpleName() + " e";
            System.out.println("Preloading entities for class: " + clazz.getSimpleName());

            List<?> result = entityManager.createQuery(jpql, clazz).getResultList();
            Method idGetter = findIdGetter(clazz);

            if (idGetter == null) {
                System.err.println("ID getter not found for class: " + clazz.getSimpleName());
                return;
            }

            HoarderCache.preload(clazz, result, entity -> {
                try {
                    return idGetter.invoke(entity);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to invoke ID getter for entity: " + clazz.getSimpleName(), e);
                }
            });

            System.out.println("Successfully preloaded " + result.size() + " entities for class: " + clazz.getSimpleName());

        } catch (Exception e) {
            System.err.println("Failed to preload entities for class: " + clazz.getSimpleName() + ", Error: " + e.getMessage());
        }
    }

    private Method findIdGetter(Class<?> clazz) {
        // First, check methods for @Id annotation (method-level JPA annotation)
        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(Id.class)) {
                return method;
            }
        }

        // Then, check fields for @Id annotation and find corresponding getter
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                String fieldName = field.getName();
                String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

                try {
                    return clazz.getMethod(getterName);
                } catch (NoSuchMethodException e) {
                    // Try boolean getter pattern (isXxx for boolean fields)
                    if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                        String booleanGetterName = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                        try {
                            return clazz.getMethod(booleanGetterName);
                        } catch (NoSuchMethodException ignored) {
                            // Continue to next field
                        }
                    }
                }
            }
        }

        return null;
    }
}