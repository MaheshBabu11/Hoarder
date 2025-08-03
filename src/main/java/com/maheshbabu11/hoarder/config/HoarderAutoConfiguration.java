package com.maheshbabu11.hoarder.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@AutoConfiguration
@ConditionalOnClass(
    name = {
      "org.springframework.data.repository.CrudRepository",
      "jakarta.persistence.EntityManager"
    })
@EnableConfigurationProperties(HoarderProperties.class)
@ComponentScan(basePackages = "com.maheshbabu11.hoarder")
@EnableAspectJAutoProxy
public class HoarderAutoConfiguration {}
