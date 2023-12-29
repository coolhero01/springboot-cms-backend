package com.oneclicktech.spring.config;


import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = { "com.oneclicktech.spring" }, excludeFilters = {
  @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = { PersistenceConfig.class })
})
public class PersistenceAutoConfig {

}
