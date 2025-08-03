package com.maheshbabu11.hoarder.aspect;

import com.maheshbabu11.hoarder.annotation.Hoarded;
import com.maheshbabu11.hoarder.config.HoarderProperties;
import com.maheshbabu11.hoarder.core.HoarderCache;
import com.maheshbabu11.hoarder.util.HoarderLogger;
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
  private final HoarderProperties hoarderProperties;
  private final HoarderLogger hoarderLogger;
  private final HoarderCache hoarderCache;

  public HoarderAspect(
      EntityManager entityManager,
      HoarderProperties hoarderProperties,
      HoarderLogger hoarderLogger,
      HoarderCache hoarderCache) {
    this.entityManager = entityManager;
    this.hoarderProperties = hoarderProperties;
    this.hoarderLogger = hoarderLogger;
    this.hoarderCache = hoarderCache;
    initializeHoardedEntityCache();
  }

  private void initializeHoardedEntityCache() {
    if (!hoarderProperties.getCache().isEnabled()) {
      hoarderLogger.info(
          HoarderAspect.class, "Hoarder cache is disabled, skipping initialization.");
      return;
    }
    Set<EntityType<?>> entityTypes = entityManager.getMetamodel().getEntities();

    for (EntityType<?> entityType : entityTypes) {
      Class<?> javaType = entityType.getJavaType();

      if (javaType.isAnnotationPresent(Hoarded.class)) {
        hoardedEntityCache.put(javaType.getSimpleName(), javaType);
        hoarderLogger.info(
            HoarderAspect.class,
            "Registered hoarded entity for aspect: {}",
            javaType.getSimpleName());
      }
    }
    hoarderLogger.info(
        HoarderAspect.class, "Initialized {} hoarded entities", hoardedEntityCache.size());
  }

  @Around("execution(* org.springframework.data.repository.CrudRepository+.findById(..))")
  public Object interceptFindById(ProceedingJoinPoint pjp) throws Throwable {
    if (!hoarderProperties.getCache().isEnabled()) {
      return pjp.proceed();
    }
    Object[] args = pjp.getArgs();
    Object target = pjp.getTarget();
    String entityClassName = getEntityClassName(target);
    hoarderLogger.debug(
        HoarderAspect.class, "Intercepted findById call for entity class: {}", entityClassName);

    Class<?> entityClass = hoardedEntityCache.get(entityClassName);
    if (entityClass == null) {
      hoarderLogger.debug(
          HoarderAspect.class, "No hoarded entity found for class: {}", entityClassName);
      return pjp.proceed();
    }

    hoarderLogger.debug(
        HoarderAspect.class, "Found hoarded entity class: {}", entityClass.getName());

    Object id = args[0];
    Optional<?> cached = hoarderCache.get(entityClass, id);
    if (cached.isPresent()) {
      hoarderLogger.debug(
          HoarderAspect.class, "Returning cached entity for {} with id: {}", entityClassName, id);
      return cached;
    }

    hoarderLogger.debug(
        HoarderAspect.class,
        "No cached entity found, executing database query for {} with id: {}",
        entityClassName,
        id);
    Object result = pjp.proceed();

    if (result instanceof Optional<?> opt && opt.isPresent()) {
      hoarderCache.put(entityClass, id, opt.get());
      hoarderLogger.debug(
          HoarderAspect.class, "Cached entity for {} with id: {}", entityClassName, id);
    }

    return result;
  }

  private String getEntityClassName(Object repository) {
    hoarderLogger.trace(
        HoarderAspect.class,
        "Extracting entity class name from repository: {}",
        repository.getClass().getName());

    try {
      Class<?>[] proxyInterfaces = repository.getClass().getInterfaces();
      hoarderLogger.trace(HoarderAspect.class, "Proxy interfaces: {}", (Object) proxyInterfaces);

      for (Class<?> proxyInterface : proxyInterfaces) {
        if (proxyInterface.getName().startsWith("org.springframework")
            && !CrudRepository.class.isAssignableFrom(proxyInterface)) {
          continue;
        }

        if (CrudRepository.class.isAssignableFrom(proxyInterface)
            || JpaRepository.class.isAssignableFrom(proxyInterface)) {
          String interfaceName = proxyInterface.getSimpleName();
          hoarderLogger.trace(HoarderAspect.class, "Found repository interface: {}", interfaceName);
          if (interfaceName.endsWith("Repository")) {
            String entityName =
                interfaceName.substring(0, interfaceName.length() - "Repository".length());
            hoarderLogger.trace(
                HoarderAspect.class, "Extracted entity name from interface name: {}", entityName);
            return entityName;
          }
        }
      }

    } catch (Exception e) {
      hoarderLogger.warn(
          HoarderAspect.class,
          "Error extracting entity class name from repository proxy: {}",
          e.getMessage());
    }

    hoarderLogger.debug(
        HoarderAspect.class, "Could not extract entity class name from proxy, returning 'Unknown'");
    return "Unknown";
  }
}
