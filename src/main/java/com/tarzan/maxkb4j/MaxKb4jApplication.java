package com.tarzan.maxkb4j;

import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;


@EnableAsync
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

    @Bean
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure().dataSource(dataSource).load();
    }

}
