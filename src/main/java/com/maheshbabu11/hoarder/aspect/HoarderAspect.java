package com.maheshbabu11.hoarder.aspect;

import com.maheshbabu11.hoarder.annotation.Hoarded;
import com.maheshbabu11.hoarder.core.HoarderCache;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.*;
import java.util.Optional;

@Aspect
@Component
public class HoarderAspect {

    @Around("execution(* org.springframework.data.repository.CrudRepository+.findById(..))")
    public Object interceptFindById(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Object[] args = pjp.getArgs();
        Object target = pjp.getTarget();

        Class<?> repoClass = target.getClass();
        Class<?> entityClass = resolveEntityClass(repoClass);

        if (entityClass == null || !entityClass.isAnnotationPresent(Hoarded.class)) {
            return pjp.proceed();
        }

        Object id = args[0];
        Optional<?> cached = HoarderCache.get(entityClass, id);
        if (cached.isPresent()) {
            return cached;
        }

        Object result = pjp.proceed();
        if (result instanceof Optional<?> opt && opt.isPresent()) {
            HoarderCache.put(entityClass, id, opt.get());
        }
        return result;
    }

    private Class<?> resolveEntityClass(Class<?> repoClass) {
        for (Type type : repoClass.getGenericInterfaces()) {
            if (type instanceof ParameterizedType pt &&
                    pt.getTypeName().contains("CrudRepository")) {
                return (Class<?>) pt.getActualTypeArguments()[0];
            }
        }
        return null;
    }
}
