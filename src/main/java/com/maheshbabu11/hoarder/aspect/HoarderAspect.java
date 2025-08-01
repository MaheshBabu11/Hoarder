package com.maheshbabu11.hoarder.aspect;

import com.maheshbabu11.hoarder.annotation.Hoarded;
import com.maheshbabu11.hoarder.core.HoarderCache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class HoarderAspect {

    private final EntityManager entityManager;
    private final Map<String, Class<?>> hoardedEntityCache = new ConcurrentHashMap<>();

    public HoarderAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
        initializeHoardedEntityCache();
    }

    private void initializeHoardedEntityCache() {
        Set<EntityType<?>> entityTypes = entityManager.getMetamodel().getEntities();

        for (EntityType<?> entityType : entityTypes) {
            Class<?> javaType = entityType.getJavaType();

            if (javaType.isAnnotationPresent(Hoarded.class)) {
                hoardedEntityCache.put(javaType.getSimpleName(), javaType);
                System.out.println("Registered hoarded entity for aspect: " + javaType.getSimpleName());
            }
        }
    }

    @Around("execution(* org.springframework.data.repository.CrudRepository+.findById(..))")
    public Object interceptFindById(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        Object target = pjp.getTarget();
        String entityClassName = getEntityClassName(target);
        System.out.println("Intercepted findById call for entity class: " + entityClassName);

        // Check if this entity class is hoarded
        Class<?> entityClass = hoardedEntityCache.get(entityClassName);
        if (entityClass == null) {
            System.out.println("No hoarded entity found for class: " + entityClassName);
            return pjp.proceed(); // not a hoarded entity
        }

        System.out.println("Found hoarded entity class: " + entityClass.getName());

        Object id = args[0];
        Optional<?> cached = HoarderCache.get(entityClass, id);
        if (cached.isPresent()) {
            System.out.println("Returning cached entity for " + entityClassName + " with id: " + id);
            return cached;
        }

        System.out.println("No cached entity found, executing database query for " + entityClassName + " with id: " + id);
        Object result = pjp.proceed();

        if (result instanceof Optional<?> opt && opt.isPresent()) {
            HoarderCache.put(entityClass, id, opt.get());
            System.out.println("Cached entity for " + entityClassName + " with id: " + id);
        }

        return result;
    }

    private String getEntityClassName(Object repository) {
        System.out.println("Extracting entity class name from repository: " + repository.getClass().getName());

        try {
            Class<?>[] proxyInterfaces = repository.getClass().getInterfaces();
            System.out.println("Proxy interfaces: " + java.util.Arrays.toString(proxyInterfaces));

            for (Class<?> proxyInterface : proxyInterfaces) {
                if (proxyInterface.getName().startsWith("org.springframework") &&
                        !CrudRepository.class.isAssignableFrom(proxyInterface)) {
                    continue;
                }

                if (CrudRepository.class.isAssignableFrom(proxyInterface) ||
                        JpaRepository.class.isAssignableFrom(proxyInterface)) {
                    String interfaceName = proxyInterface.getSimpleName();
                    System.out.println("Found repository interface: " + interfaceName);
                    if (interfaceName.endsWith("Repository")) {
                        String entityName = interfaceName.substring(0, interfaceName.length() - "Repository".length());
                        System.out.println("Extracted entity name from interface name: " + entityName);
                        return entityName;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Error extracting entity class name from repository proxy: " + e.getMessage());
        }

        System.out.println("Could not extract entity class name from proxy, returning 'Unknown'");
        return "Unknown";
    }
}