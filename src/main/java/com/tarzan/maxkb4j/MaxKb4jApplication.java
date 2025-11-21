package com.tarzan.maxkb4j;

import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@EnableCaching
@SpringBootApplication(exclude = ThymeleafAutoConfiguration.class)
public class MaxKb4jApplication  {

    public static void main(String[] args) {
        ApplicationContext context =SpringApplication.run(MaxKb4jApplication.class, args);
        Flyway flyway = context.getBean(Flyway.class);
        flyway.repair();
        flyway.migrate();
    }



}
