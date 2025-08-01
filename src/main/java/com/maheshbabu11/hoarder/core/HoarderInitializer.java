package com.maheshbabu11.hoarder.core;

import com.maheshbabu11.hoarder.annotation.Hoarded;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Component
public class HoarderInitializer {
    private static final Logger logger = LoggerFactory.getLogger(HoarderInitializer.class);

    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        logger.info("Initializing Hoarder cache...");

        // Get all repository beans
        String[] repositoryBeanNames = applicationContext.getBeanNamesForType(CrudRepository.class);

        for (String repoName : repositoryBeanNames) {
            CrudRepository<?, ?> repository = (CrudRepository<?, ?>) applicationContext.getBean(repoName);

            // Find the entity class for this repository
            Class<?> entityClass = findEntityClass(repository.getClass());
            if (entityClass == null) {
                continue;
            }

            // Check if the entity is annotated with @Hoarded
            if (!entityClass.isAnnotationPresent(Hoarded.class)) {
                logger.debug("Entity {} is not annotated with @Hoarded, skipping", entityClass.getName());
                continue;
            }

            logger.info("Preloading entities for class: {}", entityClass.getName());

            try {
                // Find the ID field
                Field idField = findIdField(entityClass);
                if (idField == null) {
                    logger.warn("No @Id field found in class {}, skipping", entityClass.getName());
                    continue;
                }

                // Get the getter method for the ID field
                String getterName = "get" + Character.toUpperCase(idField.getName().charAt(0)) + idField.getName().substring(1);
                Method getter = entityClass.getMethod(getterName);

                // Load all entities using the repository
                Iterable<?> entities = repository.findAll();
                List<Object> entityList = new ArrayList<>();
                entities.forEach(entityList::add);

                // Cache all entities
                HoarderCache.preload(entityClass, entityList, entity -> {
                    try {
                        return getter.invoke(entity);
                    } catch (Exception e) {
                        logger.error("Failed to extract ID from entity", e);
                        return null;
                    }
                });

                logger.info("Preloaded {} entities of type {}", entityList.size(), entityClass.getName());
            } catch (Exception e) {
                logger.error("Failed to preload entities of type " + entityClass.getName(), e);
            }
        }

        logger.info("Hoarder cache initialization complete");
    }

    private Field findIdField(Class<?> entityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(jakarta.persistence.Id.class)) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    private Class<?> findEntityClass(Class<?> repoClass) {
        // Check interfaces
        for (Type genericInterface : repoClass.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType pt) {
                Type rawType = pt.getRawType();
                if (rawType instanceof Class &&
                        (CrudRepository.class.isAssignableFrom((Class<?>) rawType))) {
                    Type[] typeArgs = pt.getActualTypeArguments();
                    if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                        return (Class<?>) typeArgs[0];
                    }
                }
            }
        }

        // Check superclasses recursively
        Class<?> superclass = repoClass.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            return findEntityClass(superclass);
        }

        return null;
    }
}